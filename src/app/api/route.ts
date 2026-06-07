import { NextResponse } from "next/server";

// runtime = 'edge' (Cloudflare Pages runs all routes on the edge by default)

export async function GET() {
  return NextResponse.json({ message: "Hello, world!" });
}