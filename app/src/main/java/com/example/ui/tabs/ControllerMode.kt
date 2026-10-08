package com.example.ui.tabs

/**
 * Controller App Modes:
 * - SIMPLE: Classic Ludo Board (ludo2.png reference), easy player-based input
 * - PRO: Advanced dynamic boxes B1..B6 controller with full diagnostic & telemetry controls
 */
enum class ControllerMode(val title: String, val subtitle: String) {
    SIMPLE("SIMPLE MODE", "Classic Ludo Board"),
    PRO("PRO MODE", "Advanced B1–B6 System")
}
