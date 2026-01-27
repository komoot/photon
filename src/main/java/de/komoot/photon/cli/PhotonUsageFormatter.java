package de.komoot.photon.cli;

import com.beust.jcommander.IUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;
import de.komoot.photon.config.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

import static java.lang.Boolean.FALSE;

@NullMarked
public class PhotonUsageFormatter implements IUsageFormatter {
    private static final String[] OPTION_GROUPS = {
            GeneralConfig.GROUP,
            ApiServerConfig.GROUP,
            ImportFileConfig.GROUP,
            UpdateInitConfig.GROUP,
            PostgresqlConfig.GROUP,
            ExportDumpConfig.GROUP,
            PhotonDBConfig.GROUP,
            ImportFilterConfig.GROUP
    };
    private static final String PHOTON_GENERAL_PREAMBLE = """
            Photon is an open source geocoder built for OpenStreetMap data.
            It is based on OpenSearch - an efficient, powerful and highly scalable search platform.
            
            This command-line tool allows to import, export and update
            a Photon database and to run the Photon API server.
            
            Usage: photon [-h] [<command> <options>]
            
            These are the commands available. For more information on each command
            and its options, use 'photon <command> -h'.
            
            Commands:
            """;
    private final JCommander commander;

    public PhotonUsageFormatter(JCommander commander) {
        this.commander = commander;
    }

    @Override
    public void usage(String command) {
        StringBuilder sb = new StringBuilder();
        usage(command, sb, "");
        commander.getConsole().println(sb);
    }

    @Override
    public void usage(String command, StringBuilder stringBuilder) {
        usage(command, stringBuilder, "");
    }

    @Override
    public void usage(@Nullable String command, StringBuilder stringBuilder, String indent) {
        if (command == null) {
            generalHelp(stringBuilder, indent);
        } else {
            commandHelp(command, stringBuilder, indent);
        }

    }

    @Override
    public void usage(StringBuilder stringBuilder) {
        usage(commander.getParsedCommand(), stringBuilder);

    }

    @Override
    public void usage(StringBuilder stringBuilder, String indent) {
        usage(commander.getParsedCommand(), stringBuilder, indent);
    }

    @Override
    @Nullable
    public String getCommandDescription(String command) {
        var jc = commander.findCommandByAlias(command);

        if (jc != null) {
            var p = jc.getObjects().getFirst().getClass().getAnnotation(Parameters.class);
            if (p != null) {
                return p.commandDescription();
            }
        }

        return null;
    }

    private void generalHelp(StringBuilder sb, String indent) {
        sb.append(indent);
        appendWrapped(sb, indent, PHOTON_GENERAL_PREAMBLE);
        sb.append('\n');

        int cmdlen = Arrays.stream(Commands.values())
                .map(cmd -> cmd.getCmd().length())
                .max(Integer::compare)
                .orElse(0) + 2;
        String finalIndent = indent + " ".repeat(cmdlen);

        for (var cmdEnum : Commands.values()) {
            String command = cmdEnum.getCmd();
            sb.append(indent)
                    .append(command)
                    .repeat(" ", cmdlen - command.length());
            String descr = getCommandDescription(command);
            if (descr != null) {
                appendWrapped(sb, finalIndent, descr.split("\n\n", 2)[0]);
            } else {
                sb.append('\n');
            }
        }
    }

    private void commandHelp(String command, StringBuilder sb, String indent) {
        sb.append(indent).append("Usage: photon ").append(command).append(" [options]\n\n");
        String descr = getCommandDescription(command);

        if (descr != null) {
            appendWrapped(sb, indent, descr);
            sb.append('\n');
        }

        var jc = commander.findCommandByAlias(command);
        if (jc != null) {
            String optionIndent = indent + "  ";
            String optionDescrIndent = indent + " ".repeat(24);
            for (var category : OPTION_GROUPS) {
                boolean hasHeading = false;

                for (var param : jc.getFields().values()) {
                    var annotation = param.getParameterAnnotation();
                    if (!annotation.hidden()
                            && annotation.names().length > 0
                            && category.equals(param.getCategory())) {
                        if (!hasHeading) {
                            sb.append(indent).append(category).append(":\n");
                            hasHeading = true;
                        }
                        // We only show the first name, as aliases are only used for deprecation.
                        String name = annotation.names()[0];
                        if (!annotation.placeholder().isEmpty()) {
                            name = String.join(" ", name, annotation.placeholder());
                        }
                        sb.append(optionIndent).append(name);
                        if (name.length() > 20) {
                            sb.append('\n').append(optionDescrIndent);
                        } else {
                            sb.repeat(" ", 22 - name.length());
                        }
                        String optDescr = param.getDescription();
                        if (optDescr != null) {
                            if (param.getDefault() != null && param.getDefault() != FALSE) {
                                optDescr += " (default: " + param.getDefaultValueDescription().toString() + ")";
                            }
                            appendWrapped(sb, optionDescrIndent, optDescr);
                        }
                    }
                }
                if (hasHeading) {
                    sb.append("\n");
                }
            }
        }
    }

    private void appendWrapped(StringBuilder sb, String indent, String description) {
        int lineSize = commander.getColumnSize() - indent.length();

        for (String para: description.split("\n\n")) {
            int currentLen = 0;
            for (String word: para.split("[\n ]+")) {
                if (currentLen + word.length() > lineSize) {
                    sb.append('\n').append(indent);
                    currentLen = 0;
                }
                if (currentLen > 0) {
                    sb.append(' ');
                    ++currentLen;
                }
                sb.append(word);
                currentLen += word.length();
            }
            sb.append("\n\n");
        }
        sb.delete(sb.length() - 1, sb.length());
    }
}
