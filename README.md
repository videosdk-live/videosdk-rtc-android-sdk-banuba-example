# videosdk-rtc-android-sdk-banuba-example

Quick start example to integrate Banuba SDK with videosdk.live using Android SDK to enhance video calls with real-time face filters and virtual backgrounds.

## Steps to Integrate
### Prerequisites
- Development environment requirements:
  - [Java Development Kit](https://www.oracle.com/java/technologies/downloads/)
  - Android Studio 3.0 or later
- A physical or virtual mobile device running Android 5.0 or later
- Valid [Video SDK Account](https://app.videosdk.live/)
- Valid Banuba Token and Banuba .aar files

## Run the Sample Project
### Step 1: Clone the sample project
Clone the repository to your local environment.
```js
git clone https://github.com/videosdk-live/videosdk-rtc-android-sdk-banuba-example.git
```

### Step 2: Banuba Token
To start working with the Banuba SDK, you need to have the client token. To receive the client token please fill form on [banuba.com](https://www.banuba.com/)

### Step 3: Add Banuba SDK dependencies
- With the client token, you will also receive the Banuba SDK archive for Android which contains Banuba Effect Player (compiled Android library project with .aar extension), Banuba SDK (compiled Android library project with .aar extension) and example filters located under effects folder. 

- Create `libs` directory in your project and add `banuba_effect_player.aar`, `banuba_sdk.aar` files. 

### Step 4: Modify Banuba Token
Copy and Paste your client token to `KEY` variable of [`app/src/main/java/banuba/BanubaProcessor.java`](../main/app/src/main/java/live/videosdk/rtc/android/java/banuba/BanubaProcessor.java)

### Step 5: Add Effects
Add effects that you want to use in the [`app/src/main/assets/bnb-resources/effects`](../../tree/main/app/src/main/assets/bnb-resources/effects) folder.You can download test effects here: [Effects](https://docs.banuba.com/face-ar-sdk-v1/overview/demo_face_filters)

### Step 6: Modify local.properties
Generate temporary token from [Video SDK Account](https://app.videosdk.live/signup).
```js title="local.properties"
auth_token= "TEMPORARY-TOKEN"
```

### Step 7: Run the sample app
Run the android app with **Shift+F10** or the ** ▶ Run ** from toolbar. 

## Examples
- [Prebuilt SDK Examples](https://github.com/videosdk-live/videosdk-rtc-prebuilt-examples)
- [JavaScript SDK Example](https://github.com/videosdk-live/videosdk-rtc-javascript-sdk-example)
- [React JS SDK Example](https://github.com/videosdk-live/videosdk-rtc-react-sdk-example)
- [React Native SDK Example](https://github.com/videosdk-live/videosdk-rtc-react-native-sdk-example)
- [Flutter SDK Example](https://github.com/videosdk-live/videosdk-rtc-flutter-sdk-example)
- [Android SDK Example](https://github.com/videosdk-live/videosdk-rtc-android-java-sdk-example)
- [iOS SDK Example](https://github.com/videosdk-live/videosdk-rtc-ios-sdk-example)

## Documentation
[Read the documentation](https://docs.videosdk.live/) to start using Video SDK.

## Community
- [Discord](https://discord.gg/Gpmj6eCq5u) - To get involved with the Video SDK community, ask questions and share tips.
- [Twitter](https://twitter.com/video_sdk) - To receive updates, announcements, blog posts, and general Video SDK tips.
