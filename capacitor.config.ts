import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.zixo.call',
  appName: 'Zixo',
  webDir: 'www',
  server: {
    url: 'https://zixocall.eu.cc',
    androidScheme: 'https',
    // Allow navigation to Google OAuth and Firebase auth domains
    allowNavigation: [
      'accounts.google.com',
      'zixo-call.firebaseapp.com',
    ],
  },
  android: {
    backgroundColor: '#1a1a2e',
    allowMixedContent: true,
  },
};

export default config;
