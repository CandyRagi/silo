# Silo Mobile

Silo Mobile is the companion Android application for [Silo Desktop](https://github.com/CandyRagi/silo-desktop). It enables fast local file sharing, remote trackpad control, and low-latency camera streaming directly to your PC — with no cloud, no accounts, and no internet connection required. Files stream over TCP for reliable delivery at any size, while discovery, pairing, remote input, and camera frames use lightweight UDP.

## Features

- **Reliable Local Transfers** — Send photos, videos, and files directly to your PC at local network speeds. File data streams over a dedicated TCP channel, so transfers complete intact regardless of file size.
- **Remote Trackpad** — Turn your Android device into a low-latency remote mouse and keyboard for your computer.
- **Live Viewfinder** — Stream your device camera directly to a desktop window over the local network.
- **Complete Privacy** — No cloud servers, no accounts, and no data tracking. Connections are exclusively peer-to-peer over your local network.

## Installation

### Download the Application

1. Download the latest APK from the [v1.0.1 release](https://github.com/CandyRagi/silo/releases/tag/v1.0.1), or visit the [official website](https://candyragi.github.io/silo-website).
2. Transfer the APK to your Android device or download it directly from your mobile browser.
3. Tap the APK file to install it. You may be prompted to allow installations from unknown sources in your device settings.

### Build from Source

For developers who wish to compile the Android application locally:

1. Clone this repository to your local machine.
2. Open the project directory using Android Studio.
3. Allow Gradle to sync and download all necessary dependencies.
4. Connect your Android device via USB debugging or select an emulator.
5. Click "Run" in Android Studio to build and deploy the application to your device.

## Setup Instructions

1. Install and launch [Silo Desktop](https://github.com/CandyRagi/silo-desktop) on your Windows computer.
2. Open the Silo Mobile application on your Android device.
3. Ensure both the computer and the Android device are connected to the exact same local area network.
4. On the mobile application, tap the button to scan for nearby devices.
5. Select your computer from the list of discovered devices to initiate a connection.
6. Accept the pairing prompt on your computer to begin transferring files or controlling the desktop.

## System Requirements

- Operating System: Android 10 (Q) or later
- Network: Local Area Network connection (Wi-Fi or Ethernet)
- Companion App: [Silo Desktop](https://github.com/CandyRagi/silo-desktop) installed on a target Windows machine

## Permissions

Silo Mobile requests the following device permissions, used strictly for the features described above:

- **Storage / Media access** — to select and send files to your PC.
- **Camera** — to stream the live viewfinder to your desktop.
- **Network access** — for local peer-to-peer discovery and control over UDP and file transfer over TCP. No internet access is required.

## License

This project is licensed under the MIT License.
