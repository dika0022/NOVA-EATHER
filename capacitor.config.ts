import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.aethernova.app",
  appName: "AetherNova",
  webDir: "out",
  server: {
    androidScheme: "https",
  },
};

export default config;
