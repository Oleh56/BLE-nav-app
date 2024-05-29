# Project Description

An app which uses BLE beacons for estimating position. 
RSSI values are processed by a Kalman filter, a distance approximation formula is applied to them, and finally trilateration is performed.  
Then, the user's position is set on a map using the coordinates from trilateration.   
App itself is developed for one of the University buildings, so you may need to adjust it to your needs.     

## App Workflow 

<img src="assets/app_work.gif" width="400" height="800">

## Installation

1. Clone the repository to the desired folder:

   ```bash
   cd /your/desired/folder
   git clone https://github.com/Oleh56/BLE-nav-app.git
   
2. Open project in android studio

## References
SVGMapView engine from https://github.com/jiahuanyu/SVGMapView

## License
This project is licensed under the [MIT License](LICENSE).
