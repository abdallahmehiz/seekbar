# SeekBar - Compose Multiplatform

A customizable seekbar component for Jetpack Compose Multiplatform with advanced features for media players and progress indicators.

## Features

- **Multiplatform**: Android, iOS, JVM, Web
- **Segmented Progress**: Multiple segments with gaps and custom colors
- **Markers**: Custom markers with overlay content
- **Read-Ahead Progress**: Show buffered content
- **Haptic Feedback**: Configurable haptic responses
- **Custom Styling**: Colors, dimensions, animations
- **Thumb Overlay**: Custom content following the thumb

## Installation

```kotlin
dependencies {
    implementation("io.github.abdallahmehiz:seekbar:1.1.1")
}
```

## Basic Usage

```kotlin
import live.mehiz.seekbar.SeekBar

@Composable
fun BasicExample() {
    var value by remember { mutableStateOf(0.5f) }
    
    SeekBar(
        value = value,
        onValueChange = { value = it },
        valueRange = 0f..1f
    )
}
```
