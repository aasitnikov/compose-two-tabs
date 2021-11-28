package com.example.composetwotabs

import android.os.SystemClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.gestures.OverScrollConfiguration
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.composetwotabs.ui.theme.AppTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager

// copied from
// https://github.com/onebone/compose-collapsing-toolbar/blob/6f938d7/lib/src/main/java/me/onebone/toolbar/Internals.kt
internal class RelativeVelocityTracker {

    private val tracker = VelocityTracker()
    private var lastY: Float? = null

    fun delta(delta: Float) {
        val new = (lastY ?: 0f) + delta

        tracker.addPosition(SystemClock.uptimeMillis(), Offset(0f, new))
        lastY = new
    }

    fun reset(): Float {
        lastY = null

        val velocity = tracker.calculateVelocity()
        tracker.resetTracking()

        return velocity.y
    }
}

// kinda broken because of https://issuetracker.google.com/issues/179417109
internal class ExitUntilCollapsedNestedScrollConnection(
    private val lazyListState: LazyListState,
    private val flingBehavior: FlingBehavior
) : NestedScrollConnection {

    private val tracker = RelativeVelocityTracker()

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y
        tracker.delta(dy)

        val consume = if (dy < 0) { // collapsing: toolbar -> body
            lazyListState.dispatchRawDelta(-dy)
        } else {
            0f
        }

        return Offset(0f, -consume)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val dy = available.y

        val consume = if (dy > 0) { // expanding: body -> toolbar
            lazyListState.dispatchRawDelta(-dy)
        } else {
            0f
        }

        return Offset(0f, -consume)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        println("onPreFling")
        val velocity = tracker.reset()

        val left = if (velocity < 0) {
            -fling(-velocity)
        } else {
            velocity
        }

        return Velocity(x = 0f, y = available.y - left)
    }

    private suspend fun fling(velocity: Float): Float {
        println("fling $velocity")
        var result = velocity
        lazyListState.scroll {
            val remaining = with(flingBehavior) { performFling(velocity) }
            result = remaining
        }
        return result
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        println("onPostFlin")
        val velocity = available.y

        val left = if (velocity > 0) {
            -fling(-velocity)
        } else {
            velocity
        }

        return Velocity(x = 0f, y = available.y - left)
    }
}



@Composable
fun ScreenContent(modifier: Modifier = Modifier) {
    val state = rememberLazyListState()
    val flingBehavior = ScrollableDefaults.flingBehavior()
    LazyColumn(
        state = state,
        flingBehavior = flingBehavior,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(remember { ExitUntilCollapsedNestedScrollConnection(state, flingBehavior) })
    ) {
        item {
            Box(
                Modifier
                    .background(Color(0xFF622E7E))
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                Column(Modifier.padding(start = 16.dp)) {
                    repeat(5) {
                        Box(Modifier.height(100.dp)) {
                            Text("${it + 1}")
                        }
                    }
                }
            }
        }
        item {
            TabsContent(Modifier.fillParentMaxSize())
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun TabsContent(modifier: Modifier = Modifier) {
    EnableOverScroll(enabled = false) {
        HorizontalPager(count = 2, modifier.fillMaxWidth()) { page ->
            when (page) {
                0 -> LeftTabContent()
                1 -> RightTabContent()
                else -> error("Wrong page: $page")
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LeftTabContent() {
    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            Text(LoremIpsum, Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
        }
        item {
            Spacer(Modifier.height(32.dp))
        }
        item {
            Text(
                "Title 1",
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Companion.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.W700
            )
        }
        item {
            Spacer(Modifier.height(16.dp))
        }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(5) {
                    Box(
                        Modifier
                            .background(Color.DarkGray)
                            .size(150.dp)
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(32.dp))
        }
        item {
            Text(
                "Title 2",
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Companion.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.W700
            )
        }
        item {
            Spacer(Modifier.height(16.dp))
        }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(5) {
                    Box(
                        Modifier
                            .background(Color.DarkGray)
                            .size(150.dp)
                    )
                }
            }
        }
        items(10) {
            Box(
                Modifier
                    .padding(top = 40.dp, start = 16.dp)
                    .size(8.dp)
                    .background(Color.Black)
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RightTabContent() {
    LazyColumn(Modifier.fillMaxWidth()) {
        items(100) {
            ListItem {
                Text("Item #${it + 1}")
            }
        }
    }
}

@Preview
@Composable
private fun LeftTabContentPreview() {
    AppTheme(darkTheme = true) {
        LeftTabContent()
    }
}

@Preview
@Composable
private fun RightTabContentPreview() {
    AppTheme(darkTheme = true) {
        RightTabContent()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnableOverScroll(enabled: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalOverScrollConfiguration provides (if (enabled) OverScrollConfiguration() else null),
        content = content
    )
}

private val LoremIpsum =
    """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras ac interdum magna, id imperdiet velit. Nullam rutrum nulla eleifend tristique ullamcorper. Aliquam feugiat nunc in risus tincidunt, at elementum nulla lobortis. Morbi faucibus tincidunt ligula, in mattis dui vulputate sit amet. Duis lobortis sapien orci. Fusce facilisis ipsum non mi vestibulum tristique. Duis mattis eros et lectus rhoncus, vel sodales felis aliquam. Nullam tincidunt vel dolor efficitur consectetur. Quisque ornare enim id lectus tincidunt eleifend. Curabitur malesuada nisl vitae rhoncus sodales. """