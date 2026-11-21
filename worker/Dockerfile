FROM ubuntu:24.04
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip openjdk-17-jre-headless libstdc++6 libgcc-s1 libgl1 libpulse0 libasound2 && rm -rf /var/lib/apt/lists/*
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/emulator:${ANDROID_HOME}/platform-tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdtools.zip && unzip -q /tmp/cmdtools.zip -d ${ANDROID_HOME}/cmdline-tools && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && rm /tmp/cmdtools.zip && yes | sdkmanager --licenses >/dev/null && sdkmanager 'platform-tools' 'emulator' 'system-images;android-35;google_apis;x86_64'
RUN echo 'no' | avdmanager create avd -n romstudio_api_35 -k 'system-images;android-35;google_apis;x86_64' --device 'pixel_2' --force
WORKDIR /worker
COPY package.json ./
RUN npm install --omit=dev
COPY server.js ./
EXPOSE 8787
CMD ["npm", "start"]
