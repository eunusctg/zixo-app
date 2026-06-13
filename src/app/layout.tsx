import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Zixo - Free Video and Audio Calling App",
  description: "Connect freely. No social clutter. Free video and audio calling app with end-to-end encryption.",
  manifest: "/manifest.json",
  applicationName: "Zixo",
  appleWebApp: {
    capable: true,
    title: "Zixo",
    statusBarStyle: "black-translucent",
  },
  formatDetection: {
    telephone: false,
  },
  openGraph: {
    title: "Zixo - Free Video and Audio Calling App",
    description: "Connect freely. No social clutter. Free video and audio calling with end-to-end encryption.",
    siteName: "Zixo",
    images: [{ url: "/icon-512.png", width: 512, height: 512, alt: "Zixo App" }],
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "Zixo - Free Video and Audio Calling App",
    description: "Connect freely. No social clutter. Free video and audio calling with end-to-end encryption.",
    images: [{ url: "/icon-512.png", width: 512, height: 512, alt: "Zixo App" }],
  },
  icons: {
    icon: [
      { url: "/icon-32.png", sizes: "32x32", type: "image/png" },
      { url: "/icon-48.png", sizes: "48x48", type: "image/png" },
      { url: "/icon-96.png", sizes: "96x96", type: "image/png" },
      { url: "/icon-192.png", sizes: "192x192", type: "image/png" },
    ],
    apple: [
      { url: "/icon-152.png", sizes: "152x152", type: "image/png" },
      { url: "/icon-180.png", sizes: "180x180", type: "image/png" },
    ],
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
  themeColor: [
    { media: "(prefers-color-scheme: dark)", color: "#0A0A1A" },
    { media: "(prefers-color-scheme: light)", color: "#25D366" },
  ],
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500;600&display=swap"
          rel="stylesheet"
        />
      </head>
      <body className="font-body antialiased bg-zixo-bg text-zixo-text min-h-screen">
        {children}
      </body>
    </html>
  );
}
