package com.project.sideeffects

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.sideeffects.ui.theme.SideEffectsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    //https://developer.android.com/develop/ui/compose/side-effects
    //https://medium.com/huawei-developers/jetpack-compose-side-effects-effect-handlers-ii-2cf3ca126a15
    //https://proandroiddev.com/jetpack-compose-side-effects-iii-rememberupdatedstate-c8df7b90a01d
    //https://medium.com/@mortitech/exploring-side-effects-in-compose-f2e8a8da946b

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isVisible = remember { mutableStateOf(true) }

            SideEffectsTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
//                    Counter()
//                    MyComposable()
//                    if (isVisible.value) {
//                        TimerScreen(isVisible)
//                    }
//                    Timer()
//                    TwoButtonScreen()
//                    DerivedStateExample()
                    ColorPicker()
                }
            }

        }
    }
}

//SideEffect
@Composable
fun Counter() {
    //SideEffect: publish Compose state to non-Compose code
    //To share Compose state with objects not managed by compose, use the SideEffect composable.
    //Using a SideEffect guarantees that the effect executes after every successful recomposition.
    //On the other hand, it is incorrect to perform an effect before a successful recomposition is guaranteed, which is the case when writing the effect directly in a composable.

    // Define a state variable for the count
    val count = remember { mutableStateOf(0) }

    // Use SideEffect to log the current value of count
    SideEffect {
        // Called on every recomposition
        Log.i("mLogTest", "Outer Count is ${count.value}")
    }

    Column {
        Button(onClick = { count.value++ }) {
            // Use SideEffect to log the current value of count
            SideEffect {
                // Called on every recomposition
                Log.i("mLogTest", "Inner Count is ${count.value}")
            }

            // This recomposition doesn't trigger the outer side effect
            // every time button has been tapped
            Text("Increase Count ${count.value}")
        }
    }
}

@Composable
fun MyComposable() {
    val isLoading = remember { mutableStateOf(false) }
    val data = remember { mutableStateOf(listOf<String>()) }

    // Define a LaunchedEffect to perform a long-running operation asynchronously
    // `LaunchedEffect` will cancel and re-launch if
    // `isLoading.value` changes
    LaunchedEffect(isLoading.value) {
        if (isLoading.value) {
            // Perform a long-running operation, such as fetching data from a network
            val newData = fetchData()
            // Update the state with the new data
            data.value = newData
            isLoading.value = false
        }
    }

    Column {
        Button(onClick = { isLoading.value = true }) {
            Text("Fetch Data")
        }
        if (isLoading.value) {
            // Show a loading indicator
            CircularProgressIndicator()
        } else {
            // Show the data
            LazyColumn {
                items(data.value.size) { index ->
                    Text(text = data.value[index])
                }
            }
        }
    }
}

//Simulate a network call by suspending the coroutine for 2 seconds
private suspend fun fetchData(): List<String> {
    // Simulate a network delay
    delay(2000)
    return listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
}

@Composable
fun TimerScreen(isVisible: MutableState<Boolean>) {
    //DisposableEffect: effects that require cleanup
    //For side effects that need to be cleaned up after the keys change or if the composable leaves the Composition, use DisposableEffect.
    //If the DisposableEffect keys change, the composable needs to dispose (do the cleanup for) its current effect, and reset by calling the effect again.

    val elapsedTime = remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val scope = CoroutineScope(Dispatchers.Default)
        val job = scope.launch {
            while (true) {
                delay(1000)
                elapsedTime.value += 1
                Log.i("mLogTest", "Timer is still working ${elapsedTime.value}")
            }
        }

        onDispose {
            Log.i("mLogTest", "onDispose")
            job.cancel()
        }
    }

    Text(
        text = "Elapsed Time: ${elapsedTime.value}",
        modifier = Modifier
            .padding(16.dp)
            .clickable { isVisible.value = false },
        fontSize = 24.sp
    )
}

//rememberUpdatedState
@Composable
fun TwoButtonScreen() {
    //Even though the user clicked on the buttons multiple time, the colour value was printed as Unknown.
    //Why did this happen?
    //This is because when the LaunchedEffect block started its execution, the value of colour variable was set to Unknown at the time.
    //Later, when the user clicked on the buttons, even though the Timer composable recomposed with the new colour value, the LaunchedEffect block did not restart (key1 is Unit).
    //Hence, the value of buttonColour variable inside that block never got updated.
    //
    //This is the kind of situation wherein rememberUpdatedState comes to our rescue.
    //We can use rememberUpdatedState to wrap this colour variable in a state object which will always hold the latest value the Timer composable was composed with.
    //In the example below, instead of directly referencing the colour variable, we first wrap it in rememberUpdatedState and use that instance inside the LaunchedEffect block.
    val buttonColour = remember { mutableStateOf("Unknown") }
    Column {
        Button(
            onClick = {
                buttonColour.value = "Red"
            },
            colors = ButtonDefaults.buttonColors(
                Color.Red
            )
        ) {
            Text("Red Button")
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                buttonColour.value = "Black"
            },
            colors = ButtonDefaults.buttonColors(
                Color.Black
            )
        ) {
            Text("Black Button")
        }
        Timer(buttonColour.value)
    }
}

@Composable
fun Timer(
    buttonColour: String
) {
    val timerDuration = 5000L
    Log.i("mLogTest", "Composing timer with colour : $buttonColour")
    val buttonColorUpdated by rememberUpdatedState(newValue = buttonColour)
    LaunchedEffect(key1 = Unit, block = {
        startTimer(timerDuration) {
            Log.i("mLogTest", "Timer ended")
            Log.i("mLogTest", "Last pressed button color was $buttonColour")
            Log.i("mLogTest", "[2] Last pressed button color is $buttonColorUpdated")
        }
    })
    LaunchedEffect(buttonColorUpdated) {
        Log.e("mLogTest", "$buttonColorUpdated")
    }
}

suspend fun startTimer(time: Long, onTimerEnd: () -> Unit) {
    delay(timeMillis = time)
    onTimerEnd()
}

enum class SortOrder { Name, Length, Reverse }

@SuppressLint("UnrememberedMutableState")
@Composable
fun DerivedStateExample() {
    //DerivedStateOf allows creating a state object that is derived based on a specific input state. This feature is useful in cases where multiple states are interdependent.
    //
    //DerivedStateOf acts as a lambda function that takes one or more state objects as input. The lambda function uses one or more input states to create a new derived state object.
    //
    //Let’s create an example application that allows sorting a product list in different ways and changing the background color based on the selected sorting method.
    //In this example, the variable that holds the possibilities for sorting the product list and the product list itself are interdependent.
    //First, let’s look at our example and then I’ll explain it to you.
    //The SortedItems state object is derived using derivedStateOf from the sortOrder and items state objects.
    //The sortedItems variable is automatically updated when sortOrder or items changes and sorts the items according to the new sorting order.
    //
    //In addition, the background color of each item is determined based on the sortedItems state object.
    //This means that when the sorting order of the items is changed, the background colors are also automatically updated.
    //
    //In this example, the derivedStateOf function is a powerful feature that can be used for situations where multiple state objects are dependent on each other.
    //If you encounter similar situations in your application, you can use derivedStateOf to write cleaner and more modular code.

    val sortOrder = remember { mutableStateOf(SortOrder.Name) }
    val items = remember { mutableStateListOf("apple", "banana", "cherry", "date", "elderberry") }

    val sortedItems = derivedStateOf {
        when (sortOrder.value) {
            SortOrder.Name -> items.sorted()
            SortOrder.Length -> items.sortedBy { it.length }
            SortOrder.Reverse -> items.sorted().reversed()
        }
    }

    val itemColor: @Composable (String) -> Color = { item ->
        when (sortedItems.value.indexOf(item) % 2) {
            0 -> Color.White
            1 -> Color.LightGray
            else -> Color.Transparent
        }
    }

    Column {
        Row {
            Button(onClick = { sortOrder.value = SortOrder.Name }) { Text("Sort by Name") }
            Button(onClick = { sortOrder.value = SortOrder.Length }) { Text("Sort by Length") }
            Button(onClick = { sortOrder.value = SortOrder.Reverse }) { Text("Reverse") }
        }
        LazyColumn {
            items(sortedItems.value) { item ->
                Text(
                    text = item,
//                    color = itemColor(item),
                    modifier = Modifier.padding(16.dp).background(itemColor(item))
                )
            }
        }
    }
}

@Composable
fun ColorPicker(colors: List<Color> = listOf(Color.Red, Color.Blue, Color.Green)) {
    //Snapshotflow
    //
    //SnapshotFlow is used to calculate the difference between the previous and current state using flows.
    //It is a helper class provided by Compose and can be used to create flows that monitor state objects.
    //
    //When subscribing to a flow, SnapshotFlow takes a snapshot of the current state and then watches for any changes in that state.
    //If the state changes, it takes a new snapshot and calculates the difference between the new and previous snapshots.
    //This allows for improved performance by only redrawing the parts that have changed.

    //In this example, a color picker component named ColorPicker is created.
    //A mutableState object named selectedColor holds the selected color.
    //
    //Using snapShotFlow, a flow dependent on the selectedColor state object is created.
    //This flow is created by transforming the selected colors into a flow.
    // The collectAsState function is used to store the last selected color state object. This object is used as the background color.
    //
    //The Box component is set as the background color using the lastSelectedColor object. Additionally, it is made clickable and selects the next color in the list with each click.
    //
    //This example demonstrates the use of SnapshotFlow to get colors from a dynamic data source instead of a static list.

    val selectedColor = remember { mutableStateOf(colors.first()) }

    val selectedColorFlow = snapshotFlow { selectedColor.value }

    val lastSelectedColor = selectedColorFlow.collectAsState(selectedColor.value)

    Box(
        Modifier
            .fillMaxSize()
            .background(lastSelectedColor.value)
            .clickable {
                val index = (colors.indexOf(lastSelectedColor.value) + 1) % colors.size
                selectedColor.value = colors[index]
            }
    ) {
        Text("Click to change color", modifier = Modifier.align(Alignment.Center))
    }
}