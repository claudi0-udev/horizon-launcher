package com.horizon.launcher.gamepad

import android.view.KeyEvent

enum class GamepadAction(
    val id: String,
    val displayName: String,
    val defaultKeyCode: Int,
    val description: String
) {
    CONFIRM_LAUNCH(
        id = "confirm_launch",
        displayName = "Aceptar / Iniciar",
        defaultKeyCode = KeyEvent.KEYCODE_BUTTON_A,
        description = "Abre la app/juego o confirma la selección"
    ),
    BACK_CANCEL(
        id = "back_cancel",
        displayName = "Atrás / Cancelar",
        defaultKeyCode = KeyEvent.KEYCODE_BUTTON_B,
        description = "Cierra paneles o regresa a la pantalla anterior"
    ),
    CYCLE_CATEGORY(
        id = "cycle_category",
        displayName = "Cambiar Categoría",
        defaultKeyCode = KeyEvent.KEYCODE_BUTTON_Y,
        description = "Pasa entre Más Usadas, Emuladores, Juegos y Apps"
    ),
    TOGGLE_THEME(
        id = "toggle_theme",
        displayName = "Alternar Tema",
        defaultKeyCode = KeyEvent.KEYCODE_BUTTON_X,
        description = "Cambia entre modo oscuro y claro"
    ),
    TAB_PREV(
        id = "tab_prev",
        displayName = "Pestaña Anterior (L1)",
        defaultKeyCode = KeyEvent.KEYCODE_BUTTON_L1,
        description = "Retrocede a la categoría previa a la izquierda"
    ),
    TAB_NEXT(
        id = "tab_next",
        displayName = "Pestaña Siguiente (R1)",
        defaultKeyCode = KeyEvent.KEYCODE_BUTTON_R1,
        description = "Avanza a la categoría siguiente a la derecha"
    ),
    RAM_BOOSTER(
        id = "ram_booster",
        displayName = "Game Booster RAM",
        defaultKeyCode = KeyEvent.KEYCODE_BUTTON_L2,
        description = "Libera memoria RAM rápidamente"
    ),
    QUICK_MENU(
        id = "quick_menu",
        displayName = "Menú Rápido Consola",
        defaultKeyCode = KeyEvent.KEYCODE_BUTTON_SELECT,
        description = "Abre el panel lateral de ajustes rápidos"
    ),
    DPAD_UP(
        id = "dpad_up",
        displayName = "Cruceta Arriba",
        defaultKeyCode = KeyEvent.KEYCODE_DPAD_UP,
        description = "Mueve el foco hacia arriba"
    ),
    DPAD_DOWN(
        id = "dpad_down",
        displayName = "Cruceta Abajo",
        defaultKeyCode = KeyEvent.KEYCODE_DPAD_DOWN,
        description = "Mueve el foco hacia abajo"
    ),
    DPAD_LEFT(
        id = "dpad_left",
        displayName = "Cruceta Izquierda",
        defaultKeyCode = KeyEvent.KEYCODE_DPAD_LEFT,
        description = "Mueve el foco hacia la izquierda"
    ),
    DPAD_RIGHT(
        id = "dpad_right",
        displayName = "Cruceta Derecha",
        defaultKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
        description = "Mueve el foco hacia la derecha"
    );

    companion object {
        fun fromId(id: String): GamepadAction? = values().firstOrNull { it.id == id }

        fun getKeyName(keyCode: Int): String {
            return when (keyCode) {
                KeyEvent.KEYCODE_BUTTON_A -> "Botón A"
                KeyEvent.KEYCODE_BUTTON_B -> "Botón B"
                KeyEvent.KEYCODE_BUTTON_X -> "Botón X"
                KeyEvent.KEYCODE_BUTTON_Y -> "Botón Y"
                KeyEvent.KEYCODE_BUTTON_L1 -> "L1 / LB"
                KeyEvent.KEYCODE_BUTTON_R1 -> "R1 / RB"
                KeyEvent.KEYCODE_BUTTON_L2 -> "L2 / LT"
                KeyEvent.KEYCODE_BUTTON_R2 -> "R2 / RT"
                KeyEvent.KEYCODE_BUTTON_START -> "Start / Menú"
                KeyEvent.KEYCODE_BUTTON_SELECT -> "Select / Back"
                KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3 (Stick Izq.)"
                KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3 (Stick Der.)"
                KeyEvent.KEYCODE_DPAD_UP -> "D-Pad Arriba"
                KeyEvent.KEYCODE_DPAD_DOWN -> "D-Pad Abajo"
                KeyEvent.KEYCODE_DPAD_LEFT -> "D-Pad Izquierda"
                KeyEvent.KEYCODE_DPAD_RIGHT -> "D-Pad Derecha"
                KeyEvent.KEYCODE_ENTER -> "Enter"
                KeyEvent.KEYCODE_BACK -> "Atrás"
                KeyEvent.KEYCODE_MENU -> "Menú"
                0 -> "Sin asignar"
                else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
            }
        }
    }
}
