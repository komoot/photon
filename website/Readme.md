website for photon.komoot.de

### Installation

It has been developed with python3.4 (but should work with python2.x). We suggest to use virtualenv for the installation.

* Get the virtualenv system packages:
  ```
  sudo apt-get install python-pip python-virtualenv virtualenvwrapper
  ```
* Create a virtualenv:
 ```
 mkvirtualenv -p python3.4 photon
 ```
* Install dependencies:
 ```
 cd website/photon
 pip install -r requirements.txt
 ```
* Run the server
 ```
 python app.py
 ```
* Go to http://localhost:5001/ and test it!