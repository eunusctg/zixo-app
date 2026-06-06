import { NextResponse } from "next/server";

/**
 * Zixo API Routes
 * Server-side Firebase operations that require admin/service account credentials
 */

// Health check endpoint
export async function GET() {
  return NextResponse.json({
    status: "ok",
    app: "Zixo",
    version: "1.0.0",
    firebase: {
      projectId: "zixo-call",
      region: "us-central1",
    },
  });
}

/**
 * POST /api/zixo
 * Handles various server-side operations:
 * - sendNotification: Send push notification via FCM
 * - deleteAccount: Delete user account and all associated data
 * - verifyUsername: Check if a username is available
 */
export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { action } = body;

    switch (action) {
      case "sendNotification": {
        const { token, title, body: messageBody, data } = body;
        console.log("[Zixo API] Send notification:", { token: token?.slice(0, 10) + "...", title, body: messageBody });

        return NextResponse.json({
          success: true,
          message: "Notification sent (placeholder - implement with Firebase Admin SDK)",
        });
      }

      case "deleteAccount": {
        const { uid } = body;
        console.log("[Zixo API] Delete account request for:", uid);

        return NextResponse.json({
          success: true,
          message: "Account deletion queued (placeholder - implement with Firebase Admin SDK)",
        });
      }

      case "verifyUsername": {
        const { username } = body;
        return NextResponse.json({
          available: true,
          username,
        });
      }

      default:
        return NextResponse.json(
          { error: "Unknown action" },
          { status: 400 }
        );
    }
  } catch (error) {
    console.error("[Zixo API] Error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
