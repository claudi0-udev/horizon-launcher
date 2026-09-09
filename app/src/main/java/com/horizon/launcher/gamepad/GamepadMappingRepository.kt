package com.horizon.launcher.gamepad

import android.content.Context
import android.content.SharedPreferences
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent

data class GamepadDeviceInfo(
    val id: Int,
    val name: String,
    val descriptor: String
)

class GamepadMappingRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("horizon_gamepad_mappings", Context.MODE_PRIVATE)

    fun getConnectedGamepads(): List<GamepadDeviceInfo> {
        val im = context.getSystemService(Context.INPUT_SERVICE) as? InputManager ?: return emptyList()
        val deviceIds = im.inputDeviceIds
        val list = mutableListOf<GamepadDeviceInfo>()

        for (id in deviceIds) {
            val device = im.getInputDevice(id) ?: continue
            val sources = device.sources
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
                    (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)

            if (isGamepad && !device.isVirtual) {
                list.add(
                    GamepadDeviceInfo(
                        id = device.id,
                        name = device.name,
                        descriptor = device.descriptor
                    )
                )
            }
        }
        return list
    }

    fun getMapping(descriptor: String, action: GamepadAction): Int {
        val key = "map_${cleanDescriptor(descriptor)}_${action.id}"
        return prefs.getInt(key, action.defaultKeyCode)
    }

    fun setMapping(descriptor: String, action: GamepadAction, keyCode: Int) {
        val key = "map_${cleanDescriptor(descriptor)}_${action.id}"
        prefs.edit().putInt(key, keyCode).apply()
    }

    fun resetMappings(descriptor: String) {
        val prefix = "map_${cleanDescriptor(descriptor)}_"
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach {
            editor.remove(it)
        }
        editor.apply()
    }

    fun applyPreset(descriptor: String, presetName: String) {
        when (presetName.lowercase()) {
            "switch" -> {
                // Switch Style: A is East (Confirm), B is South (Cancel), X is North (Theme), Y is West (Category)
                setMapping(descriptor, GamepadAction.CONFIRM_LAUNCH, KeyEvent.KEYCODE_BUTTON_B) // In standard Android, B is East, A is South
                setMapping(descriptor, GamepadAction.BACK_CANCEL, KeyEvent.KEYCODE_BUTTON_A)
                setMapping(descriptor, GamepadAction.TOGGLE_THEME, KeyEvent.KEYCODE_BUTTON_Y)
                setMapping(descriptor, GamepadAction.CYCLE_CATEGORY, KeyEvent.KEYCODE_BUTTON_X)
                setMapping(descriptor, GamepadAction.TAB_PREV, KeyEvent.KEYCODE_BUTTON_L1)
                setMapping(descriptor, GamepadAction.TAB_NEXT, KeyEvent.KEYCODE_BUTTON_R1)
                setMapping(descriptor, GamepadAction.RAM_BOOSTER, KeyEvent.KEYCODE_BUTTON_L2)
                setMapping(descriptor, GamepadAction.QUICK_MENU, KeyEvent.KEYCODE_BUTTON_SELECT)
            }
            "xbox" -> {
                // Xbox standard: A is South (Confirm), B is East (Cancel), X is West (Theme), Y is North (Category)
                setMapping(descriptor, GamepadAction.CONFIRM_LAUNCH, KeyEvent.KEYCODE_BUTTON_A)
                setMapping(descriptor, GamepadAction.BACK_CANCEL, KeyEvent.KEYCODE_BUTTON_B)
                setMapping(descriptor, GamepadAction.TOGGLE_THEME, KeyEvent.KEYCODE_BUTTON_X)
                setMapping(descriptor, GamepadAction.CYCLE_CATEGORY, KeyEvent.KEYCODE_BUTTON_Y)
                setMapping(descriptor, GamepadAction.TAB_PREV, KeyEvent.KEYCODE_BUTTON_L1)
                setMapping(descriptor, GamepadAction.TAB_NEXT, KeyEvent.KEYCODE_BUTTON_R1)
                setMapping(descriptor, GamepadAction.RAM_BOOSTER, KeyEvent.KEYCODE_BUTTON_L2)
                setMapping(descriptor, GamepadAction.QUICK_MENU, KeyEvent.KEYCODE_BUTTON_SELECT)
            }
            "playstation" -> {
                // PlayStation: Cross is South (Confirm), Circle is East (Cancel), Square is West (Theme), Triangle is North (Category)
                setMapping(descriptor, GamepadAction.CONFIRM_LAUNCH, KeyEvent.KEYCODE_BUTTON_A)
                setMapping(descriptor, GamepadAction.BACK_CANCEL, KeyEvent.KEYCODE_BUTTON_B)
                setMapping(descriptor, GamepadAction.TOGGLE_THEME, KeyEvent.KEYCODE_BUTTON_X)
                setMapping(descriptor, GamepadAction.CYCLE_CATEGORY, KeyEvent.KEYCODE_BUTTON_Y)
                setMapping(descriptor, GamepadAction.TAB_PREV, KeyEvent.KEYCODE_BUTTON_L1)
                setMapping(descriptor, GamepadAction.TAB_NEXT, KeyEvent.KEYCODE_BUTTON_R1)
                setMapping(descriptor, GamepadAction.RAM_BOOSTER, KeyEvent.KEYCODE_BUTTON_L2)
                setMapping(descriptor, GamepadAction.QUICK_MENU, KeyEvent.KEYCODE_BUTTON_SELECT)
            }
            else -> resetMappings(descriptor)
        }
    }

    fun resolveAction(descriptor: String?, keyCode: Int): GamepadAction? {
        val cleanDesc = descriptor?.let { cleanDescriptor(it) } ?: "default"

        // Check each action to see if mapped to this keyCode
        for (action in GamepadAction.values()) {
            val mappedKey = prefs.getInt("map_${cleanDesc}_${action.id}", -1)
            if (mappedKey != -1 && mappedKey == keyCode) {
                return action
            }
        }

        // Fallbacks for unmapped keys
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> GamepadAction.CONFIRM_LAUNCH
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK -> GamepadAction.BACK_CANCEL
            KeyEvent.KEYCODE_BUTTON_Y -> GamepadAction.CYCLE_CATEGORY
            KeyEvent.KEYCODE_BUTTON_X -> GamepadAction.TOGGLE_THEME
            KeyEvent.KEYCODE_BUTTON_L1 -> GamepadAction.TAB_PREV
            KeyEvent.KEYCODE_BUTTON_R1 -> GamepadAction.TAB_NEXT
            KeyEvent.KEYCODE_BUTTON_L2 -> GamepadAction.RAM_BOOSTER
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BUTTON_START -> GamepadAction.QUICK_MENU
            KeyEvent.KEYCODE_DPAD_UP -> GamepadAction.DPAD_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> GamepadAction.DPAD_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> GamepadAction.DPAD_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> GamepadAction.DPAD_RIGHT
            else -> null
        }
    }

    private fun cleanDescriptor(descriptor: String): String {
        return descriptor.replace("[^a-zA-Z0-9_]".toRegex(), "_").take(32)
    }
}
