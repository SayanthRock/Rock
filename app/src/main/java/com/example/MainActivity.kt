package com.example

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Theme Preset State
            var currentThemeName by rememberSaveable { mutableStateOf("SLATE") }
            var isDarkMode by rememberSaveable { mutableStateOf(true) }

            RockWallpapersTheme(darkTheme = isDarkMode, presetName = currentThemeName) {
                MainScreen(
                    isDarkMode = isDarkMode,
                    currentThemeName = currentThemeName,
                    onThemeToggle = { isDarkMode = !isDarkMode },
                    onPresetChange = { newPreset -> currentThemeName = newPreset }
                )
            }
        }
    }
}

// Highly descriptive, searchable wallpaper list structure
data class WallpaperItem(
    val url: String,
    val category: String,
    val title: String,
    val tags: List<String>
)

// Preset color matrices for previewing filters in standard Compose
// Maps cleanly to 5x4 matrix formatting
object FilterMatrices {
    val ORIGINAL = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
    val WARM_AMBER = floatArrayOf(
        1.15f, 0.05f, 0.0f,  0.0f, 25f,
        0.0f,  1.10f, 0.0f,  0.0f, 10f,
        0.0f,  0.0f,  0.80f, 0.0f, -15f,
        0.0f,  0.0f,  0.0f,  1.0f, 0f
    )
    val COOL_TEAL = floatArrayOf(
        0.70f, 0.0f,  0.0f,  0.0f, -10f,
        0.0f,  1.15f, 0.10f, 0.0f, 15f,
        0.0f,  0.05f, 1.30f, 0.0f, 30f,
        0.0f,  0.0f,  0.0f,  1.0f, 0f
    )
    val NEON_CYBER = floatArrayOf(
        1.30f, 0.0f,  0.25f, 0.0f, 45f,
        0.0f,  0.55f, 0.0f,  0.0f, -25f,
        0.20f, 0.0f,  1.45f, 0.0f, 65f,
        0.0f,  0.0f,  0.0f,  1.0f, 0f
    )
    val FOREST_ZEN = floatArrayOf(
        0.75f, 0.0f,  0.0f,  0.0f, -20f,
        0.0f,  1.35f, 0.0f,  0.0f, 25f,
        0.0f,  0.0f,  0.80f, 0.0f, -10f,
        0.0f,  0.0f,  0.0f,  1.0f, 0f
    )
    val ROSE_GOLD = floatArrayOf(
        1.25f, 0.08f, 0.08f, 0.0f, 24f,
        0.05f, 1.05f, 0.05f, 0.0f, 8f,
        0.0f,  0.08f, 1.15f, 0.0f, 18f,
        0.0f,  0.0f,  0.0f,  1.0f, 0f
    )
    val SEPIA = floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0.0f,   0.0f,   0.0f,   1f, 0f
    )
    val SLEEK_MONO = floatArrayOf(
        0.299f, 0.587f, 0.114f, 0f, 0f,
        0.299f, 0.587f, 0.114f, 0f, 0f,
        0.299f, 0.587f, 0.114f, 0f, 0f,
        0.0f,   0.0f,   0.0f,   1f, 0f
    )

    fun getMapping(name: String): FloatArray {
        return when (name) {
            "Warm Amber" -> WARM_AMBER
            "Cool Teal" -> COOL_TEAL
            "Neon Cyber" -> NEON_CYBER
            "Forest Zen" -> FOREST_ZEN
            "Rose Gold" -> ROSE_GOLD
            "Sepia" -> SEPIA
            "Sleek Mono" -> SLEEK_MONO
            else -> ORIGINAL
        }
    }
}

// Configurable Editor state bundle
data class EditorSettings(
    val isDepthMode: Boolean = false,
    
    // Unified parameters
    val blurValue: Float = 0f,
    val colorFilterName: String = "Original",
    val effectName: String = "None",
    
    // Depth-specific background parameters
    val bgBlurValue: Float = 16f,
    val bgColorFilterName: String = "Original",
    val bgEffectName: String = "None",
    
    // Depth-specific subject parameters
    val subjectBlurValue: Float = 0f,
    val subjectColorFilterName: String = "Original",
    val subjectEffectName: String = "None",
    val subjectShape: String = "Portal", // Portal (Circle), Shield (Hexagon), Crown (Diamond), Stadium (Capsule)
    val subjectScaleRatio: Float = 0.65f
)

// Custom shapes drawn purely in Kotlin Compose path drawing
val HexagonShape = object : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val path = androidx.compose.ui.graphics.Path().apply {
            val w = size.width
            val h = size.height
            moveTo(w * 0.5f, 0f)
            lineTo(w, h * 0.25f)
            lineTo(w, h * 0.75f)
            lineTo(w * 0.5f, h)
            lineTo(0f, h * 0.75f)
            lineTo(0f, h * 0.25f)
            close()
        }
        return Outline.Generic(path)
    }
}

val DiamondShape = object : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val path = androidx.compose.ui.graphics.Path().apply {
            val w = size.width
            val h = size.height
            moveTo(w * 0.5f, 0f)
            lineTo(w, h * 0.5f)
            lineTo(w * 0.5f, h)
            lineTo(0f, h * 0.5f)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun getShapeFromDescriptor(shapeName: String): Shape {
    return when (shapeName) {
        "Portal" -> CircleShape
        "Shield" -> HexagonShape
        "Crown" -> DiamondShape
        else -> RoundedCornerShape(percent = 50) // Stadium Capsule
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isDarkMode: Boolean,
    currentThemeName: String,
    onThemeToggle: () -> Unit,
    onPresetChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Local storage persistence using SharedPreferences
    val sharedPreferences = remember {
        context.getSharedPreferences("rockpaper_saved_walls", android.content.Context.MODE_PRIVATE)
    }
    
    // Loaded state from local storage SharedPreferences
    val savedUrls = remember {
        val savedSet = sharedPreferences.getStringSet("saved_urls", emptySet()) ?: emptySet()
        mutableStateListOf<String>().apply { addAll(savedSet) }
    }

    val toggleSaveWallpaper = remember {
        { url: String ->
            if (savedUrls.contains(url)) {
                savedUrls.remove(url)
            } else {
                savedUrls.add(url)
            }
            sharedPreferences.edit().putStringSet("saved_urls", savedUrls.toSet()).apply()
        }
    }

    // States
    var selectedPhoto by remember { mutableStateOf<Any?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Subjects") }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var customFileInputText by remember { mutableStateOf("") }
    
    // Grid customizer layout preferences
    var gridLayoutSize by rememberSaveable { mutableStateOf("STANDARD") }
    var cardAspectRatioName by rememberSaveable { mutableStateOf("Tall") }
    var cardRoundnessName by rememberSaveable { mutableStateOf("ROUNDED") }
    var showCaptions by rememberSaveable { mutableStateOf(true) }

    // Gemini Rock AI Studio state variables
    var aiModelName by rememberSaveable { mutableStateOf("gemini-3.1-flash-image-preview") }
    var aiPromptKeywords by remember { mutableStateOf("") }
    var aiGeneratedImageUri by remember { mutableStateOf<String?>(null) }
    var isAiGenerating by remember { mutableStateOf(false) }
    var aiAspectRatio by remember { mutableStateOf("9:16") }
    var aiImageSize by remember { mutableStateOf("1K") }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Subjects", "Backgrounds", "Nature", "Textures", "Saved", "Custom", "AI Studio")

    // Image Picker
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                customFileInputText = uri.toString()
                selectedPhoto = uri
                showPreview = true
            }
        }
    )

    // Base searchable database of ultra-premium wall items
    val baseWallpapers = remember {
        mutableStateListOf(
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1542362567-b054ec5f89fa?q=90&w=1200&fit=crop",
                category = "Subjects",
                title = "Neon Crimson Supercar",
                tags = listOf("car", "supercar", "vehicle", "sports car", "speed", "red")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1581333100576-b73bbe92c2cb?q=90&w=1200&fit=crop",
                category = "Subjects",
                title = "Stardust Astronaut",
                tags = listOf("astronaut", "space", "astro", "galaxy", "stars", "helmet")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1614728263952-84ea256f9679?q=90&w=1200&fit=crop",
                category = "Subjects",
                title = "Tokyo Neon Cyberpunk",
                tags = listOf("cyberpunk", "neon", "city", "street", "tokyo", "lights")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1555680202-c86f0e12f086?q=90&w=1200&fit=crop",
                category = "Subjects",
                title = "Retro Reflex Camera",
                tags = listOf("camera", "antique", "vintage", "lens", "retro")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1618083707368-b3823daa2726?q=90&w=1200&fit=crop",
                category = "Subjects",
                title = "Golden Pyrite Iron Ore Cluster",
                tags = listOf("rock", "stone", "geode", "metal", "gold", "pyrite", "mineral")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1534067783941-51c9c23ecefd?q=90&w=1200&fit=crop",
                category = "Backgrounds",
                title = "Void Obsidian Abstract",
                tags = listOf("abstract", "dark", "obsidian", "minimal", "minimalist", "stone", "rock")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1444703686981-a3abbc4d4fe3?q=90&w=1200&fit=crop",
                category = "Backgrounds",
                title = "Cosmic Nebula Space",
                tags = listOf("space", "nebula", "galaxy", "stars", "cosmo", "purple")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?q=90&w=1200&fit=crop",
                category = "Backgrounds",
                title = "Atmospheric Velvet Gradient",
                tags = listOf("gradient", "texture", "ambient", "minimal", "smooth")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1519750783826-e2420f4d6871?q=90&w=1200&fit=crop",
                category = "Backgrounds",
                title = "Cybernetic Satin Wave",
                tags = listOf("liquid", "cyber", "mesh", "texture", "minimal", "blue")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?q=90&w=1200&fit=crop",
                category = "Nature",
                title = "Yosemite Valley Mist",
                tags = listOf("valley", "nature", "mountain", "river", "sunset", "america")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?q=90&w=1200&fit=crop",
                category = "Nature",
                title = "Sunlit Pine Canopy",
                tags = listOf("forest", "nature", "tree", "sunlight", "green", "wood")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1590055531615-f16d36fed8a4?q=90&w=1200&fit=crop",
                category = "Nature",
                title = "Basalt Hexagonal Prism Pillars",
                tags = listOf("rock", "nature", "stone", "basalt", "cliff", "geography")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1605721911519-3dfeb3be25e7?q=90&w=1200&fit=crop",
                category = "Textures",
                title = "Magma Flowing Crystal Fissure",
                tags = listOf("stone", "lava", "fire", "element", "nature", "volcano", "rock", "texture")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?q=90&w=1200&fit=crop",
                category = "Textures",
                title = "Alabaster Veined Quartz",
                tags = listOf("minimal", "stone", "texture", "marble", "quartz", "white", "rock")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?q=90&w=1200&fit=crop",
                category = "Textures",
                title = "Prismatic Flowing Fluid",
                tags = listOf("liquid", "glass", "rainbow", "holographic", "texture", "color")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1569172189309-78598b46e40d?q=90&w=1200&fit=crop",
                category = "Textures",
                title = "Deep Amethyst Geode Crystals",
                tags = listOf("rock", "stone", "geode", "crystal", "amethyst", "purple", "mineral", "texture")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=90&w=1200&fit=crop",
                category = "Nature",
                title = "Alpine Glacier Peaks",
                tags = listOf("mountain", "alpine", "snow", "glacier", "cold", "sky")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?q=90&w=1200&fit=crop",
                category = "Nature",
                title = "Golden Sahara Dunes",
                tags = listOf("desert", "sand", "nature", "minimal", "sunset", "warm")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?q=90&w=1200&fit=crop",
                category = "Subjects",
                title = "Midnight Synthwave Skyline",
                tags = listOf("city", "skyline", "cyberpunk", "neon", "sunset", "buildings")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?q=90&w=1200&fit=crop",
                category = "Backgrounds",
                title = "Infinity Deep Star Dust",
                tags = listOf("space", "stars", "nebula", "cosmo", "black")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?q=90&w=1200&fit=crop",
                category = "Nature",
                title = "Dewdrop Emerald Foliage",
                tags = listOf("macro", "leaf", "nature", "emerald", "raindrop")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=90&w=1200&fit=crop",
                category = "Textures",
                title = "Frosted Ribbon Glass",
                tags = listOf("glass", "abstract", "gradient", "minimal", "texture", "pastel")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?q=90&w=1200&fit=crop",
                category = "Subjects",
                title = "Electric Guitar Spotlight",
                tags = listOf("guitar", "instrument", "rock", "music", "classic")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1550985616-10810253b84d?q=90&w=1200&fit=crop",
                category = "Subjects",
                title = "Cherry Stratocaster Head",
                tags = listOf("guitar", "instrument", "rock", "music", "fender")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?q=90&w=1200&fit=crop",
                category = "Subjects",
                title = "Pyrotechnic Arena Live Stage",
                tags = listOf("stage", "concert", "live", "rock", "music", "festival")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1506157786151-b8491531f063?q=90&w=1200&fit=crop",
                category = "Backgrounds",
                title = "Sunset Festival Stage",
                tags = listOf("stage", "concert", "live", "rock", "music")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?q=90&w=1200&fit=crop",
                category = "Textures",
                title = "Vintage Groove Turntable",
                tags = listOf("vintage", "retro", "vinyl", "music", "abstract")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=90&w=1200&fit=crop",
                category = "Subjects",
                title = "Vintage Silver Microphone",
                tags = listOf("vintage", "retro", "microphone", "studio", "music")
            ),
            WallpaperItem(
                url = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=90&w=1200&fit=crop",
                category = "Backgrounds",
                title = "Abstract Prism Concert Rays",
                tags = listOf("abstract", "gradient", "neon", "music", "beams")
            )
        )
    }

    // Filtered result logic based on Active Search Query vs. Selected Category Tab and Tag Filter
    val filteredWallpapers = remember(searchQuery, selectedCategory, baseWallpapers.size, savedUrls.size, selectedTagFilter) {
        val initialList = if (searchQuery.trim().isEmpty()) {
            if (selectedCategory == "Saved") {
                baseWallpapers.filter { savedUrls.contains(it.url) }
            } else {
                baseWallpapers.filter { it.category == selectedCategory }
            }
        } else {
            val q = searchQuery.lowercase().trim()
            baseWallpapers.filter { item ->
                item.title.lowercase().contains(q) ||
                item.category.lowercase().contains(q) ||
                item.tags.any { it.contains(q) }
            }
        }

        if (selectedTagFilter != null && selectedTagFilter != "All") {
            initialList.filter { item ->
                when (selectedTagFilter) {
                    "Guitar" -> {
                        item.tags.any { it.contains("guitar", ignoreCase = true) } ||
                        item.title.contains("guitar", ignoreCase = true)
                    }
                    "Live Stage" -> {
                        item.tags.any { it.contains("stage", ignoreCase = true) || it.contains("concert", ignoreCase = true) || it.contains("live", ignoreCase = true) } ||
                        item.title.contains("stage", ignoreCase = true) || item.title.contains("concert", ignoreCase = true)
                    }
                    "Vintage" -> {
                        item.tags.any { it.contains("vintage", ignoreCase = true) || it.contains("retro", ignoreCase = true) || it.contains("antique", ignoreCase = true) } ||
                        item.title.contains("vintage", ignoreCase = true) || item.title.contains("retro", ignoreCase = true)
                    }
                    "Abstract" -> {
                        item.tags.any { it.contains("abstract", ignoreCase = true) || it.contains("gradient", ignoreCase = true) || it.contains("fluid", ignoreCase = true) } ||
                        item.title.contains("abstract", ignoreCase = true) || item.title.contains("gradient", ignoreCase = true)
                    }
                    "Neon" -> {
                        item.tags.any { it.contains("neon", ignoreCase = true) || it.contains("cyber", ignoreCase = true) } ||
                        item.title.contains("neon", ignoreCase = true) || item.title.contains("cyber", ignoreCase = true)
                    }
                    "Space" -> {
                        item.tags.any { it.contains("space", ignoreCase = true) || it.contains("stars", ignoreCase = true) || it.contains("nebula", ignoreCase = true) || it.contains("cosm", ignoreCase = true) } ||
                        item.title.contains("space", ignoreCase = true) || item.title.contains("nebula", ignoreCase = true)
                    }
                    else -> {
                        val tLower = selectedTagFilter!!.lowercase()
                        item.tags.any { it.contains(tLower) } || item.title.lowercase().contains(tLower)
                    }
                }
            }
        } else {
            initialList
        }
    }

    var isOnlineQuerySimulating by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PAPER ROCK",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp),
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "WALLPAPERS • DESIGN SYSTEM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .size(46.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Theme & Custom Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(bottom = 12.dp)
        ) {
            
            // Search Input Block
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                shape = RoundedCornerShape(20.dp),
                placeholder = { Text("Search premium wallpapers (e.g. Astro, Car, Minimal)", fontSize = 14.sp) },
                prefix = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(20.dp)) },
                trailingIcon = { 
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            // Fast Search Tags Quick-Filter Chips
            val quickTags = listOf("Astro 🚀", "Cars 🏎️", "Neon 💜", "Minimal 🔲", "Nature 🌲")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickTags) { chip ->
                    val tagText = chip.split(" ").first()
                    val isActive = searchQuery.equals(tagText, ignoreCase = true)
                    FilterChip(
                        selected = isActive,
                        onClick = {
                            searchQuery = if (isActive) "" else tagText
                        },
                        label = { Text(chip, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Only show category tabs if the user is NOT actively searching
            if (searchQuery.trim().isEmpty()) {
                TabRow(
                    selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[categories.indexOf(selectedCategory).coerceAtLeast(0)]),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    categories.forEach { category ->
                        Tab(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = category
                                selectedTagFilter = null
                            },
                            text = {
                                Text(
                                    category.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    letterSpacing = 1.sp
                                )
                            }
                        )
                    }
                }
            } else {
                Text(
                    text = "SEARCH RESULTS FOR: \"${searchQuery.uppercase()}\"",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Tag Sorting filter buttons (e.g., 'Guitar', 'Live Stage', 'Vintage', 'Abstract')
            if (selectedCategory != "AI Studio") {
                val tagFiltersList = listOf("All", "Guitar", "Live Stage", "Vintage", "Abstract", "Neon", "Space")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SORT BY THEME OR TAG",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tagFiltersList) { tag ->
                            val isSelected = if (tag == "All") selectedTagFilter == null else selectedTagFilter == tag
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTagFilter = if (tag == "All") null else tag
                                },
                                label = { Text(tag, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // 👑 Professional Live Workspace & Layout Customizer Centered Suite
            if (selectedCategory != "AI Studio") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Customize Workspace Layout",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "LIVE DESIGN SYSTEM EDITOR",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.7.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Caption Toggle Icon Button
                            Surface(
                                onClick = { showCaptions = !showCaptions },
                                shape = CircleShape,
                                color = if (showCaptions) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (showCaptions) Icons.Default.Subtitles else Icons.Default.SubtitlesOff,
                                        contentDescription = "Toggle Captions",
                                        tint = if (showCaptions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: Grid density sizing (Large, Standard, Small)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Preview Size",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    listOf("LARGE", "STANDARD", "SMALL").forEach { opt ->
                                        val isSelected = gridLayoutSize == opt
                                        Surface(
                                            onClick = { gridLayoutSize = opt },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                            modifier = Modifier.testTag("size_option_${opt.lowercase()}")
                                        ) {
                                            Text(
                                                text = opt,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Row 2: Aspect Ratio customization (Tall, Square, Wide)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Aspect Ratio",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    listOf("Tall", "Square", "Wide").forEach { opt ->
                                        val isSelected = cardAspectRatioName == opt
                                        Surface(
                                            onClick = { cardAspectRatioName = opt },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                            modifier = Modifier.testTag("aspect_option_${opt.lowercase()}")
                                        ) {
                                            Text(
                                                text = opt.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Row 3: Card corner rounding customization (Sharp, Rounded, Circular)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Card Corner",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    listOf("SHARP", "ROUNDED", "CIRCULAR").forEach { opt ->
                                        val isSelected = cardRoundnessName == opt
                                        Surface(
                                            onClick = { cardRoundnessName = opt },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                            modifier = Modifier.testTag("roundness_option_${opt.lowercase()}")
                                        ) {
                                            Text(
                                                text = opt,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Dual Grid & Carousel presentation of wallpapers
            if (selectedCategory == "AI Studio") {
                Box(modifier = Modifier.weight(1f)) {
                    AiRockStudioLayout(
                        context = LocalContext.current,
                        scope = scope,
                        aiModelName = aiModelName,
                        onModelNameChange = { aiModelName = it },
                        aiPromptKeywords = aiPromptKeywords,
                        onPromptKeywordsChange = { aiPromptKeywords = it },
                        aiGeneratedImageUri = aiGeneratedImageUri,
                        onGeneratedUriChange = { aiGeneratedImageUri = it },
                        isAiGenerating = isAiGenerating,
                        onGeneratingChange = { isAiGenerating = it },
                        aiAspectRatio = aiAspectRatio,
                        onAspectRatioChange = { aiAspectRatio = it },
                        aiImageSize = aiImageSize,
                        onImageSizeChange = { aiImageSize = it },
                        aiErrorMessage = aiErrorMessage,
                        onErrorMessageChange = { aiErrorMessage = it },
                        baseWallpapers = baseWallpapers,
                        selectedPhotoSetter = { selectedPhoto = it },
                        showPreviewSetter = { showPreview = it },
                        selectedCategorySetter = { selectedCategory = it }
                    )
                }
            } else if (filteredWallpapers.isEmpty()) {
                // Interactive luxurious Empty Search or Saved State
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (selectedCategory == "Saved" && searchQuery.trim().isEmpty()) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.FavoriteBorder,
                                    contentDescription = "No Saved Wallpapers",
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No Saved Wallpapers",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Your saved collection is empty. Tap the heart icon on any wallpaper item to save it here for offline viewing and editing!",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else if (selectedCategory == "Custom" && searchQuery.trim().isEmpty()) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "No Custom Wallpapers",
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No Custom Wallpapers",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Import or type a local image file path or web URL in the Custom Input section below, then add it to your custom library!",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No Matches Found",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "We couldn't locate matching offline files. Let's simulated-download modern high-definition Unsplash premium assets matching this query!",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        if (isOnlineQuerySimulating) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        } else {
                            Button(
                                onClick = {
                                    isOnlineQuerySimulating = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(1200) // smooth simulation
                                        val q = searchQuery.trim().ifEmpty { "Aesthetic" }
                                        val generatedUrl = "https://images.unsplash.com/photo-${1500000000000 + Random.nextLong(1000000)}?q=80&w=800"
                                        baseWallpapers.add(
                                            WallpaperItem(
                                                url = generatedUrl,
                                                category = "Subjects",
                                                title = "Curated ${q.uppercase()} Studio",
                                                tags = listOf(q.lowercase(), "simulated", "premium")
                                            )
                                        )
                                        isOnlineQuerySimulating = false
                                        Toast.makeText(context, "Added curated result for \"$q\"", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("DOWNLOAD PREVIEWS ONLINE")
                            }
                        }
                    }
                }
            } else {
                // Primary Wallpaper Grid Area with dynamic layout customization parameters
                val targetCols = when (gridLayoutSize) {
                    "LARGE" -> 1
                    "SMALL" -> 3
                    else -> 2
                }
                val targetAspect = when (cardAspectRatioName) {
                    "Square" -> 1.0f
                    "Wide" -> 1.33f
                    else -> 0.72f
                }
                val targetRoundness = when (cardRoundnessName) {
                    "SHARP" -> 0.dp
                    "CIRCULAR" -> 99.dp
                    else -> 24.dp
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(targetCols),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredWallpapers) { item ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val isHovered by interactionSource.collectIsHoveredAsState()
                        val isPressed by interactionSource.collectIsPressedAsState()

                        // Subtle zoom animation
                        val cardScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.96f else if (isHovered) 1.04f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
                            label = "card_scale"
                        )

                        // Elegant floating shadow/elevation animation
                        val cardShadowElevation by animateDpAsState(
                            targetValue = if (isPressed) 2.dp else if (isHovered) 14.dp else 4.dp,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 250f),
                            label = "card_shadow"
                        )

                        // Beautiful accent border glow
                        val cardBorderColor by animateColorAsState(
                            targetValue = if (isPressed) {
                                MaterialTheme.colorScheme.secondary
                            } else if (isHovered) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            },
                            animationSpec = tween(durationMillis = 200),
                            label = "card_border_color"
                        )

                        val cardBorderWidth by animateDpAsState(
                            targetValue = if (isHovered || isPressed) 2.dp else 1.dp,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                            label = "card_border_width"
                        )

                        val glossAlpha by animateFloatAsState(
                            targetValue = if (isHovered || isPressed) 0.15f else 0.0f,
                            animationSpec = tween(durationMillis = 150),
                            label = "gloss_alpha"
                        )

                        Surface(
                            modifier = Modifier
                                .padding(6.dp)
                                .aspectRatio(targetAspect)
                                .graphicsLayer {
                                    scaleX = cardScale
                                    scaleY = cardScale
                                }
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = LocalIndication.current,
                                    onClick = {
                                        selectedPhoto = item.url
                                        showPreview = true
                                    }
                                ),
                            shape = RoundedCornerShape(targetRoundness),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = cardShadowElevation,
                            tonalElevation = if (isHovered) 8.dp else 0.dp,
                            border = BorderStroke(cardBorderWidth, cardBorderColor)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(item.url)
                                        .crossfade(true)
                                        .size(360, 500)
                                        .build(),
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Glossy reflective glass sweep highlight decoration on hover
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.0f),
                                                    Color.White.copy(alpha = glossAlpha),
                                                    Color.White.copy(alpha = 0.0f)
                                                ),
                                                start = androidx.compose.ui.geometry.Offset.Zero,
                                                end = androidx.compose.ui.geometry.Offset(250f, 450f)
                                            )
                                        )
                                )

                                // Favorite Heart Overlay
                                IconButton(
                                    onClick = { toggleSaveWallpaper(item.url) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(36.dp)
                                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                        .testTag("favorite_button_${item.title.replace("[^a-zA-Z0-9]".toRegex(), "_").lowercase()}")
                                ) {
                                    Icon(
                                        imageVector = if (savedUrls.contains(item.url)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite ${item.title}",
                                        tint = if (savedUrls.contains(item.url)) Color.Red else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Caption Overlay (Only show if Toggle is Enabled)
                                if (showCaptions) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                                )
                                            )
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                item.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = if (gridLayoutSize == "SMALL") 10.sp else 12.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                item.category.uppercase(),
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = if (gridLayoutSize == "SMALL") 7.sp else 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Gallery Selection Footbar & File Input Studio
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .testTag("file_input_container"),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "LOCAL CUSTOM IMAGE INPUT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Select file paths, content URIs, or select raw local files to preview & set",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Input Row with Text Field and Real-time Mini Thumbnail
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tiny thumbnail preview
                        Surface(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ) {
                            if (customFileInputText.trim().isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(customFileInputText.trim())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Active Custom Import Thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = "No image loaded",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Text Field Input element
                        OutlinedTextField(
                            value = customFileInputText,
                            onValueChange = { customFileInputText = it },
                            placeholder = { Text("content://media/... or https://...", fontSize = 12.sp) },
                            label = { Text("Local Image path / URL", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            trailingIcon = {
                                if (customFileInputText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { customFileInputText = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear path input",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("file_path_input_field"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pick File from Device Storage Button
                        Button(
                            onClick = {
                                pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("browse_device_button")
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("BROWSE IMAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Add to Library Gallery Database Button
                        Button(
                            onClick = {
                                val trimmed = customFileInputText.trim()
                                if (trimmed.isNotEmpty()) {
                                    val newCustomWall = WallpaperItem(
                                        url = trimmed,
                                        category = "Custom",
                                        title = "Imported " + trimmed.substringAfterLast("/").substringBefore("?").take(15).ifEmpty { "Wallpaper #" + (baseWallpapers.size + 1) },
                                        tags = listOf("imported", "custom", "local", "personal")
                                    )
                                    baseWallpapers.add(newCustomWall)
                                    selectedCategory = "Custom"
                                    Toast.makeText(context, "Added user image to 'Custom' tab!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid file path or click BROWSE", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(38.dp)
                                .testTag("add_to_library_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ADD TO GALLERY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Direct Studio Preview Button
                        Button(
                            onClick = {
                                val trimmed = customFileInputText.trim()
                                if (trimmed.isNotEmpty()) {
                                    selectedPhoto = trimmed
                                    showPreview = true
                                } else {
                                    Toast.makeText(context, "Please select/type an image source!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(38.dp)
                                .testTag("quick_preview_button")
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PREVIEW", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Preferences / Selection Custom Palette Theme Overlay
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = {
                Text(
                    "Settings & Custom Vibe Theme",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeToggle() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Canvas Mode", fontWeight = FontWeight.Bold)
                            Text("Swap ambient shades", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isDarkMode, onCheckedChange = { onThemeToggle() })
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                    
                    Text("Select App Color Theme Preset:", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    
                    // Display current beautiful themes to customize "the settings theme option will do the same".
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemePreset.values().forEach { preset ->
                            val isSelected = preset.name == currentThemeName
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPresetChange(preset.name) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Visual color circle previews
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(preset.primary, CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(preset.secondary, CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = preset.displayName,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("APPLY COHESIVE THEME", fontWeight = FontWeight.Black)
                }
            }
        )
    }

    // Gorgeous Custom Studio Preview Dialog
    if (showPreview && selectedPhoto != null) {
        WallpaperPreviewDialog(
            photo = selectedPhoto!!,
            onDismiss = { showPreview = false },
            onSetWallpaper = { scale, offset, settings ->
                scope.launch {
                    val success = performSetWallpaper(
                        context = context,
                        photo = selectedPhoto!!,
                        scale = scale,
                        offset = offset,
                        settings = settings
                    )
                    if (success) {
                        Toast.makeText(context, "Successfully customized and applied!", Toast.LENGTH_SHORT).show()
                        showPreview = false
                    } else {
                        Toast.makeText(context, "Failed to apply wallpaper.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

// Gorgeous comprehensive VSCO/Lightroom style studio wallpaper editor dialog
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WallpaperPreviewDialog(
    photo: Any,
    onDismiss: () -> Unit,
    onSetWallpaper: (Float, androidx.compose.ui.geometry.Offset, EditorSettings) -> Unit
) {
    // States
    var isSaving by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
    // Core Editor Setting States supporting dual channels
    var editorSettings by remember { mutableStateOf(EditorSettings()) }
    
    // Active Tab channel mapping: "MODE", "BLUR", "COLOR", "EFFECTS", "MASK SHAPE"
    var activeTab by remember { mutableStateOf("MODE") }
    
    // When editing in Depth mode, which channel are we currently modifying?
    var depthFilterTargetIsBg by remember { mutableStateOf(true) }

    // IMAGE RENDERING QUALITY state
    var selectedRenderOption by remember { mutableStateOf("HD 1080p") }

    val colorFiltersList = listOf(
        "Original", "Warm Amber", "Cool Teal", "Neon Cyber", "Forest Zen", "Rose Gold", "Sepia", "Sleek Mono"
    )
    val effectsList = listOf(
        "None", "Vignette", "High Contrast", "Invert Light", "Retro Grain", "Pixelate", "Rainbow Drift"
    )

    // Dynamic Time & Date for high fidelity simulated phone overlays
    var systemTimeText by remember { mutableStateOf("09:41") }
    val systemDateText = remember {
        val sdf = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.US)
        sdf.format(java.util.Date())
    }
    LaunchedEffect(Unit) {
        while (true) {
            val sdfClock = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            systemTimeText = sdfClock.format(java.util.Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07070B)) // Luxe deep dark backdrop
        ) {
            // Apply true window edge-to-edge layout flags inside standard dialog window
            val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? android.app.Dialog)?.window
            if (dialogWindow != null) {
                SideEffect {
                    dialogWindow.setLayout(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    dialogWindow.setFlags(
                        android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    )
                }
            }

            // 1) BACKGROUND AMBIENT GLOW/BLUR LAYER
            // We draw an ultra-blurred version of the wallpaper in the background of the dialog to create amazing depth!
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photo)
                    .crossfade(true)
                    .size(400, 800)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp)
                    .graphicsLayer { alpha = 0.45f },
                contentScale = ContentScale.Crop
            )

            // Dynamic vignette gradient on the background for contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // 2) MAIN CONTENT COLUMN (Header, Phone Frame, and Param Edit Sliders)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // A) TOP HEADER & TABS BAR
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Discard", tint = Color.White, modifier = Modifier.size(18.dp))
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "MOCKUP PREVIEW STUDIO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (editorSettings.isDepthMode) "3D DEPTH ISOLATION ACTIVE" else "DRAG/PINCH INSIDE PHONE TO PAN/ZOOM",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(
                                onClick = {
                                    isSaving = true
                                    onSetWallpaper(scale, offset, editorSettings)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp),
                                enabled = !isSaving
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(14.dp))
                                } else {
                                    Text("APPLY", fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Navigation Tabs Header
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tabsMap = listOf(
                                "MODE" to Icons.Default.Layers,
                                "BLUR" to Icons.Default.BlurOn,
                                "COLOR" to Icons.Default.Palette,
                                "EFFECTS" to Icons.Default.AutoAwesome,
                                "MASK" to Icons.Default.Portrait
                            )

                            tabsMap.forEach { (tabId, iconVal) ->
                                val currentKey = if (tabId == "MASK") "MASK SHAPE" else tabId
                                val isSel = activeTab == currentKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { activeTab = currentKey }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVal,
                                            contentDescription = tabId,
                                            tint = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = tabId,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // IMAGE QUALITY ENHANCER selection bar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Quality Selector",
                                tint = if (selectedRenderOption != "SD Preview") Color(0xFFFFB300) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RENDER QUALITY:",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            listOf("SD Preview", "HD 1080p", "UHD 4K").forEach { option ->
                                val isSel = selectedRenderOption == option
                                Surface(
                                    onClick = { selectedRenderOption = option },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
                                    border = BorderStroke(0.5.dp, if (isSel) Color.White else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .height(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 7.dp)) {
                                        Text(
                                            text = option,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSel) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // B) CENTER BEAUTIFUL SIMULATED MOBILE PHONE FRAME
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 6.0f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Physical Phone Border Container
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.95f)
                            .aspectRatio(0.48f) // Perfect 9:18.75 proportion
                            .shadow(32.dp, shape = RoundedCornerShape(42.dp), clip = false)
                            .border(
                                width = 5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF5A5A65),
                                        Color(0xFF1E1E24),
                                        Color(0xFF42424B),
                                        Color(0xFF101014)
                                    )
                                ),
                                shape = RoundedCornerShape(42.dp)
                            )
                            .clip(RoundedCornerShape(38.dp))
                            .background(Color.Black)
                    ) {
                        // --- Wallpaper Preview inside the phone screen boundaries ---
                        
                        // 1) Wallpaper Background Layer
                        val bgBlur = if (editorSettings.isDepthMode) editorSettings.bgBlurValue else editorSettings.blurValue
                        val bgColorFilter = if (editorSettings.isDepthMode) {
                            editorSettings.bgColorFilterName
                        } else {
                            editorSettings.colorFilterName
                        }
                        val bgEffect = if (editorSettings.isDepthMode) {
                            editorSettings.bgEffectName
                        } else {
                            editorSettings.effectName
                        }

                        val renderSize = when (selectedRenderOption) {
                            "SD Preview" -> Pair(400, 800)
                            "HD 1080p" -> Pair(1080, 2160)
                            else -> Pair(2160, 4320) // UHD 4K
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photo)
                                .crossfade(true)
                                .size(renderSize.first, renderSize.second)
                                .build(),
                            contentDescription = null,
                            colorFilter = ColorFilter.colorMatrix(ColorMatrix(FilterMatrices.getMapping(bgColorFilter))),
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(bgBlur.dp)
                                .graphicsLayer {
                                    if (!editorSettings.isDepthMode) {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = offset.y
                                    }
                                },
                            contentScale = ContentScale.Crop
                        )

                        // Vignette background effect
                        if (bgEffect == "Vignette") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        val r = this.size.maxDimension / 2.1f
                                        val grad = androidx.compose.ui.graphics.RadialGradientShader(
                                            colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                            center = this.size.center,
                                            radius = r
                                        )
                                        drawRect(
                                            brush = androidx.compose.ui.graphics.ShaderBrush(grad),
                                            blendMode = androidx.compose.ui.graphics.BlendMode.Multiply
                                        )
                                    }
                            )
                        }

                        // Rainbow background effect
                        if (bgEffect == "Rainbow Drift") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Red.copy(alpha = 0.15f),
                                                Color.Yellow.copy(alpha = 0.12f),
                                                Color.Green.copy(alpha = 0.15f),
                                                Color.Cyan.copy(alpha = 0.15f),
                                                Color.Magenta.copy(alpha = 0.15f)
                                            )
                                        )
                                    )
                            )
                        }

                        // Noise background effect
                        if (bgEffect == "Retro Grain") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        val rand = java.util.Random()
                                        for (i in 0..1000) {
                                            val x = rand.nextFloat() * size.width
                                            val y = rand.nextFloat() * size.height
                                            val r = rand.nextFloat() * 1.5f + 0.5f
                                            drawCircle(
                                                color = Color.White.copy(alpha = rand.nextFloat() * 0.15f),
                                                radius = r,
                                                center = androidx.compose.ui.geometry.Offset(x, y)
                                            )
                                        }
                                    }
                            )
                        }

                        // Pixelate background effect
                        if (bgEffect == "Pixelate") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        val sz = size.width
                                        val div = 22f
                                        val boxSize = sz / div
                                        for (xIndex in 0..div.toInt()) {
                                            for (yIndex in 0..(size.height / boxSize).toInt()) {
                                                if ((xIndex + yIndex) % 2 == 0) {
                                                    drawRect(
                                                        color = Color.Black.copy(alpha = 0.08f),
                                                        topLeft = androidx.compose.ui.geometry.Offset(xIndex * boxSize, yIndex * boxSize),
                                                        size = androidx.compose.ui.geometry.Size(boxSize, boxSize)
                                                    )
                                                }
                                            }
                                        }
                                    }
                            )
                        }

                        // 2) Wallpaper Foreground Cutout Layer (Exclusive inside Depth Isolation Mode)
                        if (editorSettings.isDepthMode) {
                            val shape = getShapeFromDescriptor(editorSettings.subjectShape)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth(editorSettings.subjectScaleRatio)
                                    .aspectRatio(0.68f)
                                    .graphicsLayer {
                                        translationX = offset.x / 1.3f
                                        translationY = offset.y / 1.3f
                                    }
                                    .shadow(16.dp, shape = shape, clip = false)
                                    .border(2.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f), shape)
                                    .clip(shape)
                                    .background(Color.Black)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(photo)
                                        .size(renderSize.first, renderSize.second)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.colorMatrix(ColorMatrix(FilterMatrices.getMapping(editorSettings.subjectColorFilterName))),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(editorSettings.subjectBlurValue.dp)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        },
                                    contentScale = ContentScale.Crop
                                )

                                if (editorSettings.subjectEffectName == "Vignette") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .drawBehind {
                                                val r = this.size.maxDimension / 2.3f
                                                val grad = androidx.compose.ui.graphics.RadialGradientShader(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                                    center = this.size.center,
                                                    radius = r
                                                )
                                                drawRect(brush = androidx.compose.ui.graphics.ShaderBrush(grad))
                                            }
                                    )
                                }

                                if (editorSettings.subjectEffectName == "Rainbow Drift") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Magenta.copy(alpha = 0.15f),
                                                        Color.Cyan.copy(alpha = 0.15f),
                                                        Color.Yellow.copy(alpha = 0.12f)
                                                    )
                                                )
                                            )
                                    )
                                }

                                if (editorSettings.subjectEffectName == "Retro Grain") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .drawBehind {
                                                val rand = java.util.Random()
                                                for (i in 0..300) {
                                                    val x = rand.nextFloat() * size.width
                                                    val y = rand.nextFloat() * size.height
                                                    val r = rand.nextFloat() * 1.5f + 0.5f
                                                    drawCircle(
                                                        color = Color.White.copy(alpha = rand.nextFloat() * 0.15f),
                                                        radius = r,
                                                        center = androidx.compose.ui.geometry.Offset(x, y)
                                                    )
                                                }
                                            }
                                    )
                                }
                            }
                        }

                        // --- HIGH-FIDELITY MOBILE LOCKSCREEN OVERLAYS ---

                        // A) Camera Pill (Dynamic Island Notch)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                                .size(width = 75.dp, height = 20.dp)
                                .background(Color.Black, RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        )

                        // B) Simulated Status Bar with custom battery and Signal shapes to be 100% compile-safe
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(top = 9.dp)
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = systemTimeText,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                            Row(
                                modifier = Modifier.padding(end = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Real-looking Cellular Signal Tower drawn via solid boxes
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                ) {
                                    listOf(3.dp, 5.dp, 7.dp, 9.dp).forEach { hVal ->
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(hVal)
                                                .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(0.5.dp))
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "Wifi",
                                    tint = Color.White.copy(alpha = 0.95f),
                                    modifier = Modifier.size(11.dp)
                                )

                                // Real-looking Battery drawn manually
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(9.dp)
                                        .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(2.dp))
                                        .padding(1.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(0.85f)
                                            .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(0.5.dp))
                                    )
                                }
                            }
                        }

                        // C) Central Clock & Date
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 58.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock State",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(13.dp)
                                    .padding(bottom = 2.dp)
                            )
                            Text(
                                text = systemDateText.uppercase(),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = systemTimeText,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.W200, // beautiful display thin styling
                                letterSpacing = (-1).sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // D) Flashlight & Camera Quick Buttons at bottom
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Flashlight action icon
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                    .border(1.2.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .clickable { /* Simulate click toggling device flashlight */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "Flashlight",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Camera action icon
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                    .border(1.2.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .clickable { /* Simulate click launching lockscreen camera */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // E) Home Sweeper Bar gesture line at bottom
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .size(width = 110.dp, height = 4.dp)
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
                        )
                    }
                }

                // C) BOTTOM FLOATING PARAMETER EDIT SLIDERS
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        when (activeTab) {
                            "MODE" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Split canvas for 3D depth isolation",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                            .padding(2.dp)
                                    ) {
                                        listOf(false to "Unified", true to "Depth Space").forEach { (isDepth, label) ->
                                            val isSel = editorSettings.isDepthMode == isDepth
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSel) Color.White else Color.Transparent)
                                                    .clickable { editorSettings = editorSettings.copy(isDepthMode = isDepth) }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) Color.Black else Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            "BLUR" -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (!editorSettings.isDepthMode) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "BLUR: ${editorSettings.blurValue.toInt()}%",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.width(62.dp)
                                            )
                                            Slider(
                                                value = editorSettings.blurValue,
                                                onValueChange = { editorSettings = editorSettings.copy(blurValue = it) },
                                                valueRange = 0f..50f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color.White,
                                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                                ),
                                                modifier = Modifier.weight(1f).height(24.dp)
                                            )
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "BG BLUR: ${editorSettings.bgBlurValue.toInt()}%",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.5.sp,
                                                    modifier = Modifier.width(72.dp)
                                                )
                                                Slider(
                                                    value = editorSettings.bgBlurValue,
                                                    onValueChange = { editorSettings = editorSettings.copy(bgBlurValue = it) },
                                                    valueRange = 0f..50f,
                                                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = MaterialTheme.colorScheme.primary),
                                                    modifier = Modifier.weight(1f).height(20.dp)
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "SUBJ BLUR: ${editorSettings.subjectBlurValue.toInt()}%",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.5.sp,
                                                    modifier = Modifier.width(72.dp)
                                                )
                                                Slider(
                                                    value = editorSettings.subjectBlurValue,
                                                    onValueChange = { editorSettings = editorSettings.copy(subjectBlurValue = it) },
                                                    valueRange = 0f..50f,
                                                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = MaterialTheme.colorScheme.primary),
                                                    modifier = Modifier.weight(1f).height(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            "COLOR" -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (editorSettings.isDepthMode) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Apply filter to:", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                            Row(
                                                modifier = Modifier
                                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                    .padding(2.dp)
                                            ) {
                                                listOf(true to "Background", false to "Subject").forEach { (targetBg, label) ->
                                                    val isSel = depthFilterTargetIsBg == targetBg
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (isSel) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                                            .clickable { depthFilterTargetIsBg = targetBg }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(label, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(colorFiltersList) { filterName ->
                                            val currentSelectedName = if (editorSettings.isDepthMode) {
                                                if (depthFilterTargetIsBg) editorSettings.bgColorFilterName else editorSettings.subjectColorFilterName
                                            } else {
                                                editorSettings.colorFilterName
                                            }
                                            val isSelected = filterName == currentSelectedName

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                                                    .border(0.5.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        editorSettings = if (editorSettings.isDepthMode) {
                                                            if (depthFilterTargetIsBg) {
                                                                editorSettings.copy(bgColorFilterName = filterName)
                                                            } else {
                                                                editorSettings.copy(subjectColorFilterName = filterName)
                                                            }
                                                        } else {
                                                            editorSettings.copy(colorFilterName = filterName)
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    filterName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            "EFFECTS" -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (editorSettings.isDepthMode) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Apply effect to:", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                            Row(
                                                modifier = Modifier
                                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                    .padding(2.dp)
                                            ) {
                                                listOf(true to "Background", false to "Subject").forEach { (targetBg, label) ->
                                                    val isSel = depthFilterTargetIsBg == targetBg
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (isSel) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                                            .clickable { depthFilterTargetIsBg = targetBg }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(label, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(effectsList) { effName ->
                                            val currentSelectedName = if (editorSettings.isDepthMode) {
                                                if (depthFilterTargetIsBg) editorSettings.bgEffectName else editorSettings.subjectEffectName
                                            } else {
                                                editorSettings.effectName
                                            }
                                            val isSelected = effName == currentSelectedName

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                                                    .border(0.5.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        editorSettings = if (editorSettings.isDepthMode) {
                                                            if (depthFilterTargetIsBg) {
                                                                editorSettings.copy(bgEffectName = effName)
                                                            } else {
                                                                editorSettings.copy(subjectEffectName = effName)
                                                            }
                                                        } else {
                                                            editorSettings.copy(effectName = effName)
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    effName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            "MASK SHAPE" -> {
                                if (!editorSettings.isDepthMode) {
                                    Text(
                                        "Toggle \"Depth Space\" Mode first to enable mask shape isolation.",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "MASK SIZE: ${(editorSettings.subjectScaleRatio * 100).toInt()}%",
                                                color = Color.White,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(88.dp)
                                            )
                                            Slider(
                                                value = editorSettings.subjectScaleRatio,
                                                onValueChange = { editorSettings = editorSettings.copy(subjectScaleRatio = it) },
                                                valueRange = 0.4f..0.85f,
                                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.weight(1f).height(20.dp)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("CUTOUT FORM:", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf("Portal", "Shield", "Crown", "Stadium").forEach { sName ->
                                                    val isSel = editorSettings.subjectShape == sName
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (isSel) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                                                            .border(0.5.dp, if (isSel) Color.White else Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                            .clickable { editorSettings = editorSettings.copy(subjectShape = sName) }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(sName, fontSize = 8.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Background Task applying exact modified matrix & layouts on the actual Wallpaper
private suspend fun performSetWallpaper(
    context: android.content.Context,
    photo: Any,
    scale: Float,
    offset: androidx.compose.ui.geometry.Offset,
    settings: EditorSettings
): Boolean = withContext(Dispatchers.IO) {
    try {
        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        if (screenWidth <= 0 || screenHeight <= 0) return@withContext false

        val wallpaperManager = WallpaperManager.getInstance(context)
        val imageLoader = ImageLoader(context)
        
        // Dynamic loading high performance metrics
        val request = ImageRequest.Builder(context)
            .data(photo)
            .allowHardware(false)
            .size(screenWidth, screenHeight)
            .build()

        val result = imageLoader.execute(request)
        if (result !is SuccessResult) return@withContext false

        val drawable = result.drawable
        var isCreatedBitmap = false
        val srcBitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            isCreatedBitmap = true
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }

        // Setup base composited target canvas
        val resultBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // 1) RENDER BACKGROUND LAYER
        val bgBlurVal = if (settings.isDepthMode) settings.bgBlurValue else settings.blurValue
        val bgColorFilterName = if (settings.isDepthMode) settings.bgColorFilterName else settings.colorFilterName
        val bgEffectName = if (settings.isDepthMode) settings.bgEffectName else settings.effectName

        // Process Base Scaled Background Image fitting viewport
        val tempBgBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val tempBgCanvas = Canvas(tempBgBitmap)
        val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        val bgMatrix = Matrix()

        val scX = screenWidth.toFloat() / srcBitmap.width
        val scY = screenHeight.toFloat() / srcBitmap.height
        val baseScale = maxOf(scX, scY)

        bgMatrix.postScale(baseScale, baseScale)
        bgMatrix.postTranslate((screenWidth - srcBitmap.width * baseScale) / 2f, (screenHeight - srcBitmap.height * baseScale) / 2f)

        if (!settings.isDepthMode) {
            // Apply scale and translation from layout coordinates
            bgMatrix.postScale(scale, scale, screenWidth / 2f, screenHeight / 2f)
            bgMatrix.postTranslate(offset.x, offset.y)
        }

        // Apply ColorMatrix filter mapping
        val bgMatrixVal = FilterMatrices.getMapping(bgColorFilterName)
        val androidBgMatrix = android.graphics.ColorMatrix(bgMatrixVal)
        bgPaint.colorFilter = android.graphics.ColorMatrixColorFilter(androidBgMatrix)

        tempBgCanvas.drawBitmap(srcBitmap, bgMatrix, bgPaint)

        // Apply background effects
        applyBitmapEffects(tempBgBitmap, tempBgCanvas, bgEffectName)

        // Process Soft Blur to Background Layer
        val finalBgBitmap = if (bgBlurVal > 0f) {
            val dScale = (bgBlurVal / 4.5f).toInt().coerceIn(2, 10)
            val smallW = (screenWidth / dScale).coerceAtLeast(1)
            val smallH = (screenHeight / dScale).coerceAtLeast(1)
            val smallBmp = Bitmap.createScaledBitmap(tempBgBitmap, smallW, smallH, true)
            val blurred = Bitmap.createScaledBitmap(smallBmp, screenWidth, screenHeight, true)
            smallBmp.recycle()
            blurred
        } else {
            tempBgBitmap
        }

        // Draw background to main target canvas
        canvas.drawBitmap(finalBgBitmap, 0f, 0f, null)
        
        if (finalBgBitmap != tempBgBitmap) finalBgBitmap.recycle()
        tempBgBitmap.recycle()

        // 2) RENDER SUBJECT CUTOUT LAYER
        if (settings.isDepthMode) {
            val subSizeW = screenWidth * settings.subjectScaleRatio
            // Maintain exact target aspect ratio
            val subSizeH = subSizeW / 0.68f

            val left = (screenWidth - subSizeW) / 2f
            val top = (screenHeight - subSizeH) / 2f
            val right = left + subSizeW
            val bottom = top + subSizeH

            val clipPath = Path()
            when (settings.subjectShape) {
                "Portal" -> {
                    clipPath.addCircle(screenWidth / 2f, screenHeight / 2f, subSizeW / 2f, Path.Direction.CW)
                }
                "Shield" -> {
                    // Draw hexagon coordinates bounding box
                    clipPath.moveTo(left + subSizeW * 0.5f, top)
                    clipPath.lineTo(right, top + subSizeH * 0.25f)
                    clipPath.lineTo(right, top + subSizeH * 0.75f)
                    clipPath.lineTo(left + subSizeW * 0.5f, bottom)
                    clipPath.lineTo(left, top + subSizeH * 0.75f)
                    clipPath.lineTo(left, top + subSizeH * 0.25f)
                    clipPath.close()
                }
                "Crown" -> {
                    // Draw diamond bounds
                    clipPath.moveTo(left + subSizeW * 0.5f, top)
                    clipPath.lineTo(right, top + subSizeH * 0.5f)
                    clipPath.lineTo(left + subSizeW * 0.5f, bottom)
                    clipPath.lineTo(left, top + subSizeH * 0.5f)
                    clipPath.close()
                }
                else -> {
                    // Stadium capsule rounded rect
                    val rectF = RectF(left, top, right, bottom)
                    clipPath.addRoundRect(rectF, subSizeW / 2f, subSizeW / 2f, Path.Direction.CW)
                }
            }

            // Draw clean shadow/glow around cutout path
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 10f
                // Glowing aesthetic matching MaterialTheme accent color under current theme (white or custom presets)
                color = android.graphics.Color.WHITE
            }
            canvas.drawPath(clipPath, borderPaint)

            // Save state, apply path constraint
            canvas.save()
            canvas.clipPath(clipPath)

            // Process and scale subject contents nested inside path
            val subPaint = Paint(Paint.FILTER_BITMAP_FLAG)
            val subMatrix = Matrix()

            // Calculate translations relative to center clipping shape coordinates
            subMatrix.postScale(baseScale, baseScale)
            subMatrix.postTranslate((screenWidth - srcBitmap.width * baseScale) / 2f, (screenHeight - srcBitmap.height * baseScale) / 2f)
            
            // Multipolar parallax scaling translation matching Compose studio offsets
            val adjustedScaleX = scale
            subMatrix.postScale(adjustedScaleX, adjustedScaleX, screenWidth / 2f, screenHeight / 2f)
            subMatrix.postTranslate(offset.x / 1.3f, offset.y / 1.3f)

            // Subject Color Filter mapping
            val subMatrixVal = FilterMatrices.getMapping(settings.subjectColorFilterName)
            val androidSubMatrix = android.graphics.ColorMatrix(subMatrixVal)
            subPaint.colorFilter = android.graphics.ColorMatrixColorFilter(androidSubMatrix)

            // Temporal subject buffer supporting filters
            val tempSubBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
            val tempSubCanvas = Canvas(tempSubBitmap)
            tempSubCanvas.drawBitmap(srcBitmap, subMatrix, subPaint)

            // Apply subject physical rendering effects
            applyBitmapEffects(tempSubBitmap, tempSubCanvas, settings.subjectEffectName)

            val finalSubBitmap = if (settings.subjectBlurValue > 0f) {
                // Blur subject if requested
                val dScale = (settings.subjectBlurValue / 5f).toInt().coerceIn(1, 10)
                val sw = (screenWidth / dScale).coerceAtLeast(1)
                val sh = (screenHeight / dScale).coerceAtLeast(1)
                val small = Bitmap.createScaledBitmap(tempSubBitmap, sw, sh, true)
                val blurred = Bitmap.createScaledBitmap(small, screenWidth, screenHeight, true)
                small.recycle()
                blurred
            } else {
                tempSubBitmap
            }

            canvas.drawBitmap(finalSubBitmap, 0f, 0f, null)
            
            if (finalSubBitmap != tempSubBitmap) finalSubBitmap.recycle()
            tempSubBitmap.recycle()

            canvas.restore()
        }

        wallpaperManager.setBitmap(resultBitmap)

        // Memory Recycling cleanup
        if (isCreatedBitmap) srcBitmap.recycle()
        resultBitmap.recycle()
        
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// Utility class adapting custom shader designs to Bitmap drawing at high resolution
private fun applyBitmapEffects(bmp: Bitmap, canvas: Canvas, effectName: String) {
    when (effectName) {
        "Vignette" -> {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            val radialGrad = android.graphics.RadialGradient(
                w / 2f, h / 2f, maxOf(w, h) / 2.1f,
                intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK),
                floatArrayOf(0f, 0.5f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            p.shader = radialGrad
            canvas.drawRect(0f, 0f, w, h, p)
        }
        "Rainbow Drift" -> {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            val linearGrad = android.graphics.LinearGradient(
                0f, 0f, canvas.width.toFloat(), 0f,
                intArrayOf(
                    android.graphics.Color.argb(38, 255, 0, 0),    // Red
                    android.graphics.Color.argb(30, 255, 255, 0),  // Yellow
                    android.graphics.Color.argb(38, 0, 255, 0),    // Green
                    android.graphics.Color.argb(38, 0, 255, 255),  // Cyan
                    android.graphics.Color.argb(38, 255, 0, 255)   // Magenta
                ),
                null,
                android.graphics.Shader.TileMode.CLAMP
            )
            p.shader = linearGrad
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), p)
        }
        "Retro Grain" -> {
            val rand = java.util.Random()
            val p = Paint().apply {
                color = android.graphics.Color.argb(35, 255, 255, 255)
            }
            for (i in 0..4000) {
                val x = rand.nextFloat() * canvas.width
                val y = rand.nextFloat() * canvas.height
                val r = rand.nextFloat() * 2f + 0.8f
                canvas.drawCircle(x, y, r, p)
            }
        }
        "Pixelate" -> {
            // Low-res checker overlay giving blocky game styling
            val p = Paint().apply {
                color = android.graphics.Color.argb(16, 0, 0, 0)
            }
            val sizeFactor = canvas.width / 24f
            val maxRow = (canvas.width / sizeFactor).toInt()
            val maxCol = (canvas.height / sizeFactor).toInt()
            for (r in 0..maxRow) {
                for (c in 0..maxCol) {
                    if ((r + c) % 2 == 0) {
                        canvas.drawRect(
                            r * sizeFactor,
                            c * sizeFactor,
                            (r + 1) * sizeFactor,
                            (c + 1) * sizeFactor,
                            p
                        )
                    }
                }
            }
        }
    }
}

// --- Gemini API Retrofit and Moshi client models for RockPaper Rock AI Studio ---

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String
)

data class ImageConfig(
    val aspectRatio: String? = null,
    val imageSize: String? = null
)

data class GenerationConfig(
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @retrofit2.http.POST("v1beta/models/{model}:generateContent")
    suspend fun generateImage(
        @retrofit2.http.Path("model") model: String,
        @retrofit2.http.Query("key") apiKey: String,
        @retrofit2.http.Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = com.squareup.moshi.Moshi.Builder()
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

// --- Gemini Rock AI Studio Composable Layout ---

@Composable
fun AiRockStudioLayout(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    aiModelName: String,
    onModelNameChange: (String) -> Unit,
    aiPromptKeywords: String,
    onPromptKeywordsChange: (String) -> Unit,
    aiGeneratedImageUri: String?,
    onGeneratedUriChange: (String?) -> Unit,
    isAiGenerating: Boolean,
    onGeneratingChange: (Boolean) -> Unit,
    aiAspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    aiImageSize: String,
    onImageSizeChange: (String) -> Unit,
    aiErrorMessage: String?,
    onErrorMessageChange: (String?) -> Unit,
    baseWallpapers: androidx.compose.runtime.snapshots.SnapshotStateList<WallpaperItem>,
    selectedPhotoSetter: (Any?) -> Unit,
    showPreviewSetter: (Boolean) -> Unit,
    selectedCategorySetter: (String) -> Unit
) {
    val presets = listOf("Guitar", "Concert Stage", "Classic Rock", "Drum Kit", "Neon Vinyl", "Heavy Metal", "Psychedelic")
    
    val triggerGeneration = { keywords: String ->
        onGeneratingChange(true)
        onErrorMessageChange(null)
        onGeneratedUriChange(null)
        scope.launch {
            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    onErrorMessageChange("The Gemini API Key is missing or is the default placeholder! Please register/configure your actual Gemini key in the AI Studio SECRETS panel to generate wallpapers live on your emulator.")
                    onGeneratingChange(false)
                    return@launch
                }
                
                val promptText = "A professional ultra-high definition visual theme smartphone wallpaper for Rock & Roll. Keywords and elements describing composition: ${keywords.trim()}. Rich textures, dramatic stage lighting, neon colors, perfect music poster aesthetic, complete smartphone screen composition, no text, no captions."

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = promptText)))
                    ),
                    generationConfig = GenerationConfig(
                        imageConfig = ImageConfig(aspectRatio = aiAspectRatio, imageSize = aiImageSize),
                        responseModalities = listOf("TEXT", "IMAGE")
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateImage(aiModelName, apiKey, request)
                }

                val partWithImage = response.candidates?.firstOrNull()?.content?.parts?.find { it.inlineData != null }
                val b64 = partWithImage?.inlineData?.data

                if (b64 != null) {
                    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        val localFile = java.io.File(context.filesDir, "ai_wallpaper_${System.currentTimeMillis()}.jpg")
                        withContext(Dispatchers.IO) {
                            java.io.FileOutputStream(localFile).use { out ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                            }
                        }
                        val localUri = Uri.fromFile(localFile).toString()
                        onGeneratedUriChange(localUri)
                        Toast.makeText(context, "AI Wallpaper Generated!", Toast.LENGTH_LONG).show()
                    } else {
                        onErrorMessageChange("Failed to convert image bytes into native Bitmap.")
                    }
                } else {
                    val plainText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!plainText.isNullOrBlank()) {
                        onErrorMessageChange("The model returned a text reply instead of an image: \"$plainText\". Make sure your API Key has image modality access.")
                    } else {
                        onErrorMessageChange("No image data returned from the Gemini endpoint.")
                    }
                }
            } catch (e: Exception) {
                onErrorMessageChange("API Connection Error: ${e.localizedMessage ?: e.message}")
            } finally {
                onGeneratingChange(false)
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .testTag("ai_rock_studio_panel"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // Title & Concept description
            Column {
                Text(
                    text = "🎸 Gemini Rock Studio",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Synthesize unique rock-inspired wallpapers natively via Google's Gemini multimodal intelligence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. TAP KEYWORD SUGGESTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Chips row wrap or scroll row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(presets) { preset ->
                            val isSelected = aiPromptKeywords.contains(preset, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val current = aiPromptKeywords.trim()
                                    if (isSelected) {
                                        // Remove it
                                        val cleaned = current.split(",")
                                            .map { it.trim() }
                                            .filterNot { it.equals(preset, ignoreCase = true) }
                                            .joinToString(", ")
                                        onPromptKeywordsChange(cleaned)
                                    } else {
                                        // Add it
                                        if (current.isEmpty()) {
                                            onPromptKeywordsChange(preset)
                                        } else {
                                            onPromptKeywordsChange("$current, $preset")
                                        }
                                    }
                                },
                                label = { Text(preset, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Text(
                        text = "2. CUSTOMIZE PROMPT KEYWORDS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = aiPromptKeywords,
                        onValueChange = onPromptKeywordsChange,
                        placeholder = { Text("guitar solo, colorful neon smoke, rock icon...", fontSize = 13.sp) },
                        label = { Text("Rock Keywords prompt", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_prompt_text_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3. CHOOSE RENDERING CONFIGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Aspect Ratio", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("9:16", "1:1", "16:9").forEach { ratio ->
                                val active = aiAspectRatio == ratio
                                val label = when (ratio) {
                                    "9:16" -> "Portrait (9:16)"
                                    "1:1" -> "Square (1:1)"
                                    else -> "Widescreen (16:9)"
                                }
                                SuggestionChip(
                                    onClick = { onAspectRatioChange(ratio) },
                                    label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        labelColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Render Size", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("512", "1K").forEach { size ->
                                val active = aiImageSize == size
                                val label = if (size == "512") "Fast (512px)" else "Premium (1024px)"
                                SuggestionChip(
                                    onClick = { onImageSizeChange(size) },
                                    label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        labelColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Core Model", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("gemini-3.1-flash-image-preview", "gemini-2.5-flash-image").forEach { model ->
                                val active = aiModelName == model
                                val label = if (model.contains("3.1")) "Gemini 3.1 Pro" else "Gemini 2.5 (Fast)"
                                SuggestionChip(
                                    onClick = { onModelNameChange(model) },
                                    label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        labelColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // Trigger Button / Progress
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isAiGenerating) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Text(
                                "🎸 Synthesis in progress...",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Gemini is building your custom rock masterpiece. This generally takes 5-15 seconds...",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Custom prompt generation button
                        Button(
                            onClick = {
                                if (aiPromptKeywords.trim().isEmpty()) {
                                    Toast.makeText(context, "Please select suggestions or enter custom keywords!", Toast.LENGTH_SHORT).show()
                                } else {
                                    triggerGeneration(aiPromptKeywords)
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(52.dp)
                                .testTag("ai_generate_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("GENERATE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Surprise Me button
                        OutlinedButton(
                            onClick = {
                                val randomSubjects = listOf(
                                    "melting electric guitar on fire", 
                                    "amplifier blasting neon soundwaves", 
                                    "legendary vintage stage microphone", 
                                    "classic cherry stratocaster", 
                                    "vinyl record emitting golden celestial lasers", 
                                    "rock concert drum kit in glowing neon arena",
                                    "heavy metal sound mixer with colorful level needles",
                                    "flying V guitar floating in galactic space nebula",
                                    "cyberpunk rockstar playing a solo on stage"
                                )
                                val randomAccents = listOf(
                                    "mysterious smoke, cyan stage laser rays", 
                                    "dripping colorful paint, splatters", 
                                    "electric purple lightning spikes, shockwaves", 
                                    "cinematic retro sunset golden rays backdrop", 
                                    "vibrant explosion of rock dust particles", 
                                    "dramatic hyper-realistic stage spotlight",
                                    "surreal fluorescent dreamscape lighting"
                                )
                                val randomStyles = listOf(
                                    "futuristic cyberpunk music poster", 
                                    "grungy retro 1970s record album illustration", 
                                    "modern artistic high-contrast graphic concept design", 
                                    "vivid psychedelic dream-rock artwork cover", 
                                    "extreme close-up dramatic camera perspective render", 
                                    "premium studio photograph, ultra detailed lighting art style"
                                )
                                
                                val chosenPrompt = "${randomSubjects.random()}, ${randomAccents.random()}, ${randomStyles.random()}"
                                onPromptKeywordsChange(chosenPrompt)
                                triggerGeneration(chosenPrompt)
                            },
                            modifier = Modifier
                                .weight(1.0f)
                                .height(52.dp)
                                .testTag("ai_surprise_me_button"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("SURPRISE ME", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (aiErrorMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text(
                                "GENERATION FAILED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                aiErrorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        if (aiGeneratedImageUri != null) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚡ GENERATED MASTERPIECE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        // Preview Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(aiGeneratedImageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "AI Generated Custom Art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Options buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Studio Apply/Edit
                            Button(
                                onClick = {
                                    selectedPhotoSetter(aiGeneratedImageUri)
                                    showPreviewSetter(true)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("ai_open_editor_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("OPEN EDITOR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Save to favorites category
                            Button(
                                onClick = {
                                    val wallTitle = "AI Rock " + aiPromptKeywords.split(",").firstOrNull()?.trim()?.take(15)
                                    val finalTitle = wallTitle ?: "AI Rock Custom #" + (baseWallpapers.size + 1)
                                    
                                    val newWallItem = WallpaperItem(
                                        url = aiGeneratedImageUri,
                                        category = "Custom",
                                        title = finalTitle,
                                        tags = listOf("ai", "rock", "custom") + aiPromptKeywords.split(",").map { it.trim().lowercase() }
                                    )
                                    
                                    if (!baseWallpapers.any { it.url == aiGeneratedImageUri }) {
                                        baseWallpapers.add(newWallItem)
                                    }
                                    
                                    selectedCategorySetter("Custom")
                                    Toast.makeText(context, "Saved to Custom Library gallery!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("ai_save_gallery_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ADD GALLERY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


