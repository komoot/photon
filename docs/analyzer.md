# Name Analysis

Photon can apply some pre-processing to the names and address terms that it
imports in order to improve searchability of the terms. 
The name analysis is configured through a [YAML](https://yaml.org/spec/1.2.2/)
configuration file. Photon comes with a
[default configuration](https://github.com/komoot/photon/blob/master/src/main/resources/analyzer.yaml)
that should be suitable for most use cases. You can also write a custom
configuration to adapt the analyzer to your needs. This section explains
how the analyzer works and how if can be configured.

## Running imports with a custom configuration

A custom analyzer configuration can be handed in with the command-line parameter
`-analyzer`. The option must be set whenever Photon is importing data into
the OpenSearch database: during import, for updates and for serve when
update triggering via the API is enabled.

A configuration yaml file consists of three major sections:

* normalization
* language-defaults-per-country
* variants

Each of the sections is explained in detail below.

### Working with configuration includes

In some parts of the configuration it is possible to use _configuration includes_.
This allows to break up the sometimes complex configuration into smaller parts
by moving them into extra files. It also allows you to reuse parts of the builtin
configuration when writing your own configuration.

The following two syntax variants are recognised:

    /BUILTIN/ /absolute/path/to/resource.yaml

Includes a subconfiguration that comes bundled with Photon. The path must
be absolute. Check the default configuration and the
[resource directory](https://github.com/komoot/photon/tree/master/src/main/resources)
for available configurations.

    /FILE/ your/own/config.yaml

Includes a subconfiguration from a file. When the path is relative, then the
file will be loaded relative to the directory where the main analyzer config
file is in. Otherwise, the absolute name will be used.

## Normalization

Normalization transforms a name from one form to another. The original form
of the name is invariably lost after normalization.

The analyzer applies normalization at exactly one point, for _input_. Input
may be names and address terms imported with the data, but it is also any
query input during run-time.

The normalization section in the analyzer config looks as follows:

```
normalization:
  input:
    - ":: lower"
```

There is exactly one sub-section "input" right now for the normalization of
input. Normalization uses
[ICU transform rules](https://unicode-org.github.io/icu/userguide/transforms/general/).
The input sections needs to be a list where each item is either a single
ICU transform rule or a _configuration include_ containing a list of transform
rules.

For details on writing ICU transform rules, please refer to the
[ICU documentation](https://unicode-org.github.io/icu/userguide/transforms/).
Be aware that some of the rule syntax doesn't play well with YAML. Rules
should always be surrounded by quotes to avoid issues.

> [!IMPORTANT]
> In order to be able to match queries correctly with the database content, it
> is important that normalization rules are applied consistently between entering
> and querying the data. For that reason, normalization rules are only read
> once from the file during import and then saved as a property of the database.
> The normalization section of the configuration is ignored when running updates.
> Instead the normalization given during import read from the properties and used.
> If you want to change the normalization rules, you need to reimport the database.

## Language-defaults-per-country

Rules in the variant configuration below can be configured to apply only to
terms in a certain language. Sometimes a place will have language-specific
versions of its name but often it only has the name in the local language.
This is particularly the case in countries where one language is dominant.
To be able to apply language-specific rules also in such cases, you may define
default languages for countries. Any name not specifically tagged with a
language will then assumed to be in the default language.

The syntax for the language-defaults-per-country looks as follows:

```
language-defaults-per-country:
    us: en
    de: de
```

The section contains a list of mappings from 2-letter country code to
language code. Alternatively, the section may also consist of a single
_configuration include_. To use the builtin lists of language defaults:

```
language-defaults-per-country: /BUILTIN/ /language-defaults-per-country.yaml
```

## Variants

The variants transformer allows to create alternative variants and spellings
for a name, which are then all indexed in the database. This is particularly
useful to allow searching for abbreviations.

Variants are defined as a chain of _transformers_ which are applied one after
another to a given name. If a transformer produces one or more variants, each
of them is given to the next transformer. The order in which the transformers
are defined in the configuration matters.

A typical variants configuration looks like this:

```
variants:
  - type: lexical
    rules:
      - ...
  - type: icu-transform
    rules:
     - ...
```

The "variants" section contains a list of transforms where each transform
defines at least two properties: "type" and "rules". The former states what
kind of variant transformer to use, the latter contains the list of rules
to apply. The list of rules may either contain a single rule or a
_configuration include_ containing a list of rules.

### Rule filters

Every rule may have properties that define the context in which the rule is
to be applied. The following properties are currently recognised:

* **lang**: only apply the rule to names that are tagged with the given language.
  Use 'default' to apply the rule in cases where no language can be identified
  for the name. If the name comes from an object that is in a country where
  a default language is defined, then this language will be assumed for names
  without a language identifier.
* **country**: only apply the rule when the object the name belongs to is
  inside the given country. Must be a 2-letter language code.
* **categories**: a list of [categories](../categories.md). Only apply when
  the name belongs to an object that has all of the categories in the list set.
* **extra**: a mapping of strings. Only apply when the name belongs to an object
  where all of the given extra information exists and is set to the value given.
  Only works with extra information in form of a string.

### Lexical transformers

The lexical transformers adds variants for words and word prefixes and suffixes.
It is mainly suited to introduce variants for abbreviations. To use it, define
`type: lexical`.

Each rule of a lexical transformer consists of the optional filter properties
(see above) and a section "words" with the list of replacements to apply. Variants
in a rule are applied in parallel, meaning that the longest matching variant
in a word will win.

#### Replacement rules

The basic replacement description is of the form:

```
<source>[,<source>[...]] => <target>[,<target>[...]]
```

The left side contains one or more `source` terms to be replaced. The right side
lists one or more replacements. Each source is replaced with each replacement
term.

> [!TIP]
> The source and target terms are internally normalized using the input
> normalization rules given in the configuration. This ensures that the
> strings match as expected. In fact, it is better to use non-normalized
> words in the configuration because then it is possible to change the
> rules for normalization later without having to adapt the replacement rules.

###### Decomposition

In its standard form, only full words match against the source. There
is a special notation to match the prefix and suffix of a word:

``` yaml
- ~strasse => str  # matches "strasse" as full word and in suffix position
- hinter~ => hntr  # matches "hinter" as full word and in prefix position
```

There is no facility to match a string in the middle of the word.

The suffix
and prefix notation automatically trigger the decomposition mode: two variants
are created for each replacement, one with the replacement attached to the word
and one separate. So in above example, "hauptstrasse" will
get the variants "hauptstr" and "haupt str". Similarly, the name "rote strasse"
triggers the variants "rote str" and "rotestr". By having decomposition work
both ways, it is sufficient to create the variants at index time. The variant
rules are not applied at query time.

To avoid automatic decomposition, use the '|' notation:

``` yaml
- ~strasse |=> str
```

This simply changes "hauptstrasse" to "hauptstr" and "rote strasse" to "rote str".

###### Initial and final terms

It is also possible to restrict replacements to the beginning and end of a
name:

``` yaml
- ^south => s  # matches only at the beginning of the name
- road$ => rd  # matches only at the end of the name
```

So the first example would trigger a replacement for "south 45th street" but
not for "the south beach restaurant".

###### Replacements vs. variants
 
The replacement syntax `source => target` works as a pure replacement. It changes
the name instead of creating a variant. To create an additional version, you'd
have to write `source => source,target`. As this is a frequent case, there is
a shortcut notation for it:
 
```
<source>[,<source>[...]] -> <target>[,<target>[...]]
```
 
The simple arrow causes an additional variant to be added. Note that
decomposition has an effect here on the source as well. So a rule
 
``` yaml
- "~strasse -> str"
```
 
means that for a word like `hauptstrasse` four variants are created:
`hauptstrasse`, `haupt strasse`, `hauptstr` and `haupt str`.


### ICU transformers

The ICU transformer creates variants by applying ICU transform rules. It is
mostly suited to create simple spelling variants. To use it,
add `type: icu-transform`.

Each rule of a ICU transformer consists of the optional filter properties
(see above) and a section "transforms" with the list of ICU transform rules.
The rules in the list are all applied to the input word in order and the
resulting output then becomes a new variant when it differs from the input.

To give an example, the German Umlauts ä, ö, ü may also be written as ae, oe, ue.
This can be expressed in ICU transform rules like this:

```
  - type: icu-transform
    rules:
      - lang: de
        transforms:
          - ä > ae
          - ö > oe
          - ü > ue
```

(Note how the rule will only be applied to German-language words.)

A word like "Dürrröhrsdorf" will be transformed into "Duerrroehrsdorf" by those
rules. Both variants are added to the index and can be searched for without
triggering a penalty.
