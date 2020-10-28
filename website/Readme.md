## Website for photon.komoot.io

This directory contains the website for photon's home page.

### Configuration

Copy the example configuration into the photon directory:

```
cp config.js.example photon/config.js
```

Then adapt `config.js` to your needs.

### Running

The website can be directly opened in your browser or served with any
webserver that can serve static files.

If you have Python3 installed, you can run:

```
make serve
```

Then go to http://localhost:5001/ and test it!
