# Privacy Friendly Interval Timer

This Android application manages the time during interval based training sessions, eg circuit training.
It provides a configurable set of timers for the exercises and the rest phases.
The app can remind the user about planned training sessions and offers statistics such as the time spend training or calories burnt.

Privacy Friendly Interval Timer belongs to the group of Privacy Friendly Apps developed by the research group SECUSO at Karlsruhe Institute of Technology.

[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/packages/com.intervaltimer.google/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
      alt="Get it on Google Play"
      height="80">](https://play.google.com/store/apps/details?id=com.intervaltimer.google)

## Motivation

The motivation of this project is to provide users with an application which supports training sessions without advertisement or the demand of unnecessary permissions.
Privacy Friendly Apps are a group of Android applications which are optimized regarding privacy. Further information can be found on https://secuso.org/pfa

## Information

The calories calculation is based on the Metabolic Equivalent of Task (MET) formula. The MET value was taken from:
https://www.fitness-gesundheit.uni-wuppertal.de/fileadmin/fitness-gesundheit/pdf-Dokumente/Publikationen/2015/Prof.Stemper_F_G_4-15.pdf
 
### API Reference

Mininum SDK: 21
Target SDK: 29 

## License

Privacy Friendly Interval Timer is licensed under the GPLv3.
Copyright (C) 2017-2020  Alexander Karakuz, Christopher Beckmann

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.

The icons used in the nagivation drawer are licensed under the [CC BY 2.5](http://creativecommons.org/licenses/by/2.5/). In addition to them the app uses icons from [Google Design Material Icons](https://design.google.com/icons/index.html) licensed under Apache License Version 2.0. All other images (the logo of Privacy Friendly Apps, the SECUSO logo, the app icons and the splash icon) copyright [Karlsruher Institut für Technologie](www.kit.edu) (2020).

## Contributors

App-Icon: <br />
Markus Hau<br />

Github-Users: <br />
alexkarakuz <br />
Yonjuni (Karola Marky)<br />
Kamuno<br />
sleep-yearning

## Dev notes

To build:

    $ gradle wrapper build

To make keystore:

    $ keytool -genkey -v -keystore release-key.jks -validity 10000 -alias privacy-friendly-interval-timer

To sign:

    $ $ANDROID_HOME/build-tools/26.0.2/apksigner sign -ks release-key.jks --out app/build/outputs/apk/release/app-release-signed.apk app/build/outputs/apk/release/app-release-unsigned.apk
