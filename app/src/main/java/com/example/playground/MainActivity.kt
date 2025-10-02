package com.example.playground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.playground.ui.theme.PlaygroundTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlaygroundTheme {
                Scaffold( modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Main(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

enum class Mode {
    BASIC, SCIENTIFIC
}

class CalcSymbols {
    companion object {
        val numberSymbols = setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        val binaryFunSymbols = setOf("+", "-", "×", "/", "^")
        val unaryFunSymbols = setOf(
            "√", "sin", "cos", "tan", "ln", "arcsin", "arccos",
            "arctan", "x!"
        )
        val bracketSymbols = setOf("(", ")")
        val miscSymbols = setOf("C", "BS", ".", "=") // C for clear, BS for backspace
    }
}

fun isNumeric(s: String): Boolean {
    // check if a given string is numeric (incl. numbers with - prefix):
    return s.toDoubleOrNull() != null || (s.startsWith("-") && s.drop(1).toDoubleOrNull() != null)
}

fun getOperatorPrecedence(operator: String): Int {
    return when (operator) {
        "+", "-" -> 1
        "×", "/" -> 2
        "^"      -> 3
        else     -> 0
    }
}

fun factorial(a: Double): Int {
    // only works for natural numbers (including 0).
    var fac = 1
    if (a >= 0 && a.rem(1).equals(0.0)) {
        val input = a.toInt()
        for (i in 1..input) {
            fac = fac * i
        }
    }
    else {
        throw IllegalArgumentException("Invalid input.")
    }
    return fac
}

// converts expression written in an infix list to postfix list using the shunting yard algo.
fun infixToPostfix(infixTokens: List<String>): List<String> {
    val postfixQueue = ArrayDeque<String>()
    val operatorStack = ArrayDeque<String>()

    for (token in infixTokens) {
        when {
            isNumeric(token) -> { // if token is a number, enqueue
                postfixQueue.addLast(token)
            }

            token in CalcSymbols.unaryFunSymbols -> { // if the token is a unary function, add to stack
                operatorStack.addFirst(token)
            }

            token in CalcSymbols.binaryFunSymbols -> { // if the token is a binary operator,
                while ( // while the stack is not empty, its top isn't a (, and
                // operator precedence of the top is higher than the token or equal and token is not ^,
                    operatorStack.isNotEmpty() &&
                    operatorStack.first() != "(" &&
                    (getOperatorPrecedence(operatorStack.first()) > getOperatorPrecedence(token) ||
                            (getOperatorPrecedence(operatorStack.first()) == getOperatorPrecedence(token) && token != "^"))
                ) {
                    // pop the operator from the stack into the postfix
                    postfixQueue.addLast(operatorStack.removeFirst())
                }
                operatorStack.addFirst(token)
            }

            token == "(" -> {
                operatorStack.addFirst(token)
            }

            token == ")" -> { // if the token is a ),
                while (operatorStack.firstOrNull() != "(") { // while the top element is not a (,
                    // pop the top into the postfix
                    if (operatorStack.isEmpty()) {
                        break
                    }
                    postfixQueue.addLast(operatorStack.removeFirst())
                }

                if (operatorStack.isNotEmpty()) {
                    // discard the (
                    operatorStack.removeFirst()
                }

                if (operatorStack.firstOrNull() in CalcSymbols.unaryFunSymbols) { // if the top of the stack is a unary function
                    postfixQueue.addLast(operatorStack.removeFirst())  // pop the function into the postfix
                }
            }
        }
    }

    while (operatorStack.isNotEmpty()) {
        // pop remaining operator stack into postfix while it's not empty
        postfixQueue.addLast(operatorStack.removeFirst())
    }
    return postfixQueue.toList()
}

// evaluates and calculates mathematical expressions in postfix/reverse polish notation.
fun calculatePostfix(postfixTokens: List<String>): List<String> {
    val resultStack = ArrayDeque<String>()

    for (token in postfixTokens) {
        when {
            isNumeric(token) -> { // if the token is a number.
                resultStack.addFirst(token)
            }
            token in CalcSymbols.unaryFunSymbols -> { // if the encountered token is
                // a unary function symbol.
                // possible symbols: "√", "sin", "cos", "tan", "ln", "arcsin", "arccos",
                //            "arctan", "x!"
                val arg = resultStack.removeFirst().toDouble()
                val res = when (token) {
                    "√" -> sqrt(arg)
                    "sin" -> sin(arg)
                    "cos" -> cos(arg)
                    "tan" -> tan(arg)
                    "ln" -> ln(arg)
                    "arcsin" -> asin(arg)
                    "arccos" -> acos(arg)
                    "arctan" -> atan(arg)
                    "x!" -> factorial(arg)
                    else -> {
                        throw IllegalArgumentException("Invalid input.")
                    }
                }
                resultStack.addFirst(res.toString())
            }
            token in CalcSymbols.binaryFunSymbols -> { // if the encountered token
                // is a binary operator "+", "-", "×", "/", "^"
                val arg2 = resultStack.removeFirst().toDouble()
                val arg1 = resultStack.removeFirst().toDouble()

                val res = when (token) {
                    "+" -> (arg1 + arg2)
                    "-" -> (arg1 - arg2)
                    "×" -> (arg1 * arg2)
                    "/" -> (arg1 / arg2)
                    "^" -> arg1.pow(arg2)
                    else -> {
                        throw IllegalArgumentException("Invalid input.")
                    }
                }
                resultStack.addFirst(res.toString())
            }
        }
    }

    return resultStack
}

// parses inputted symbol into an infix list; returns a new infix list with the processed input symbol
fun parseInfix(currentInfix: List<String>, newSymbol: String): List<String> {
    val mutableInfix = currentInfix.toMutableList()
    val lastSymbol = mutableInfix.lastOrNull()

    when (newSymbol) {
        "C" -> return emptyList()
        "BS" -> {
            if (mutableInfix.isEmpty()) return emptyList()
            val last = mutableInfix.last()

            if (last.length > 1 && isNumeric(last)) {
                mutableInfix[mutableInfix.size - 1] = last.dropLast(1)
            } else {
                mutableInfix.removeAt(mutableInfix.size - 1)
            }
            return mutableInfix
        }
        "1/x" -> {
            mutableInfix.add("1")
            mutableInfix.add("/")
        }
        "=" -> {
            if (mutableInfix.isEmpty()) return emptyList()
            try {
                val postfix = infixToPostfix(currentInfix)
                val result = calculatePostfix(postfix)
                return result
            }
            catch (e: Exception) {
                return currentInfix
            }
        }
        in CalcSymbols.numberSymbols -> {
            if (lastSymbol == "-") {
                val secondLastSymbol = mutableInfix.getOrNull(mutableInfix.size - 2)
                if (secondLastSymbol == null || secondLastSymbol in CalcSymbols.binaryFunSymbols || secondLastSymbol == "(") {
                    mutableInfix[mutableInfix.size - 1] = lastSymbol + newSymbol
                } else {
                    mutableInfix.add(newSymbol)
                }
            } else if (lastSymbol != null && isNumeric(lastSymbol)) {
                mutableInfix[mutableInfix.size - 1] = lastSymbol + newSymbol
            } else if (lastSymbol == ")") {
                mutableInfix.add("×")
                mutableInfix.add(newSymbol)
            } else {
                mutableInfix.add(newSymbol)
            }
        }

        "." -> {
            if (lastSymbol != null && isNumeric(lastSymbol) && !lastSymbol.contains(".")) {
                mutableInfix[mutableInfix.size - 1] = "$lastSymbol."
            } else if (lastSymbol == null || lastSymbol in CalcSymbols.binaryFunSymbols || lastSymbol == "(") {
                mutableInfix.add("0.")
            }
        }

        in CalcSymbols.binaryFunSymbols -> {
            if (newSymbol == "-" && (lastSymbol == null || lastSymbol in CalcSymbols.binaryFunSymbols || lastSymbol == "(")) {
                mutableInfix.add("-")
            } else if (lastSymbol != null && (isNumeric(lastSymbol) || lastSymbol == ")")) {
                mutableInfix.add(newSymbol)
            } else if (lastSymbol != null && lastSymbol in CalcSymbols.binaryFunSymbols) {
                mutableInfix[mutableInfix.size - 1] = newSymbol
            }
        }

        in CalcSymbols.unaryFunSymbols -> {
            if (lastSymbol != null && (isNumeric(lastSymbol) || lastSymbol == ")")) {
                mutableInfix.add("×")
            }
            mutableInfix.add(newSymbol)
            mutableInfix.add("(")
        }

        "(" -> {
            if (lastSymbol != null && (isNumeric(lastSymbol) || lastSymbol == ")")) {
                mutableInfix.add("×")
            }
            mutableInfix.add(newSymbol)
        }

        ")" -> {
            val openBrackets = mutableInfix.count { it == "(" }
            val closeBrackets = mutableInfix.count { it == ")" }

            if (openBrackets > closeBrackets && lastSymbol != null && lastSymbol !in CalcSymbols.binaryFunSymbols && lastSymbol != "(") {
                mutableInfix.add(newSymbol)
            }
        }
    }

    return mutableInfix.toList()
}

fun displayInfix(currentInfix: List<String>): String {
    if (currentInfix.isEmpty()) {
        return "0"
    }
    else {
        return currentInfix.joinToString(separator = " ")
    }
}

@Composable
fun Main(modifier: Modifier) {
    var currentMode by rememberSaveable {mutableStateOf(Mode.BASIC)}
    var currentCalculator by rememberSaveable {mutableStateOf(listOf<String>())}

    fun switchMode() {
        currentMode = if (currentMode == Mode.BASIC) {
            Mode.SCIENTIFIC
        } else {
            Mode.BASIC
        }
    }

    fun processInput(symbol: String) {
        currentCalculator = parseInfix(currentCalculator, symbol)
    }

    Column(modifier) {
        NumDisplay(displayString = displayInfix(currentCalculator))
        if (currentMode == Mode.SCIENTIFIC) {
            ScientificView(currentMode, switchMode = { switchMode() }, processInput = ::processInput)
        }
        else {
            BasicView(currentMode, switchMode = {switchMode()}, processInput = ::processInput)
        }
    }
}

@Composable
fun BasicView(currentMode: Mode, switchMode: () -> Unit, modifier: Modifier = Modifier,
              processInput: (String) -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeightedSpacer()
            WeightedSpacer()
            WeightedSpacer()
            BackspaceButton(processInput = processInput)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeightedFuncButton("C", processInput = processInput)
            WeightedFuncButton("(", processInput = processInput)
            WeightedFuncButton(")", processInput = processInput)
            WeightedFuncButton("/", processInput = processInput)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeightedNumButton("7", processInput = processInput)
            WeightedNumButton("8", processInput = processInput)
            WeightedNumButton("9", processInput = processInput)
            WeightedFuncButton("×", processInput = processInput)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeightedNumButton("4", processInput = processInput)
            WeightedNumButton("5", processInput = processInput)
            WeightedNumButton("6", processInput = processInput)
            WeightedFuncButton("-", processInput = processInput)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeightedNumButton("1", processInput = processInput)
            WeightedNumButton("2", processInput = processInput)
            WeightedNumButton("3", processInput = processInput)
            WeightedFuncButton("+", processInput = processInput)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SwitchModeButton(currentMode, switchMode)
            WeightedNumButton("0", processInput = processInput)
            WeightedFuncButton(".", processInput = processInput)
            EqualsButton(processInput = processInput)
        }
    }
}

@Composable
fun ScientificView(currentMode: Mode, switchMode: () -> Unit, modifier: Modifier = Modifier,
        processInput: (String) -> Unit) {
    val smallerFont = MaterialTheme.typography.headlineSmall.fontSize

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeightedFuncButton("√", processInput = processInput)
            WeightedFuncButton("sin", processInput = processInput, fontSize = smallerFont)
            WeightedFuncButton("cos", processInput = processInput, fontSize = smallerFont)
            WeightedFuncButton("tan", processInput = processInput, fontSize = smallerFont)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeightedFuncButton("ln", processInput = processInput)
            WeightedFuncButton("arcsin", processInput = processInput, fontSize = smallerFont)
            WeightedFuncButton("arccos", processInput = processInput, fontSize = smallerFont)
            WeightedFuncButton("arctan", processInput = processInput, fontSize = smallerFont)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SwitchModeButton(currentMode, switchMode)
            WeightedFuncButton("1/x", processInput = processInput, fontSize = smallerFont)
            WeightedFuncButton("x!", processInput = processInput)
            WeightedFuncButton("^", processInput = processInput)
        }
    }
}

@Composable
fun RowScope.WeightedSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.weight(1f))
}

@Composable
fun RowScope.SwitchModeButton(currentMode: Mode, switchMode: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = switchMode, modifier = modifier.weight(1f).aspectRatio(1f)) {
        Text(text = if (currentMode == Mode.BASIC) "BASIC" else "SCI.",
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            fontWeight = FontWeight.Medium
            )
    }
}

@Composable
fun NumDisplay(modifier: Modifier = Modifier, displayString: String) {
    val scrollState = rememberScrollState()

    LaunchedEffect(displayString) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Row(modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 48.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.End
        ) {
        Text(
            text = displayString,
            fontSize = MaterialTheme.typography.displayMedium.fontSize,
            textAlign = TextAlign.End,
            softWrap = false,
            maxLines = 1
        )
    }
}

@Composable
fun RowScope.EqualsButton(modifier: Modifier = Modifier, processInput: (String) -> Unit) {
    Button(onClick = { processInput("=") }, modifier = modifier.weight(1f).aspectRatio(1f)) {
        ButtonText("=", modifier = modifier, fontSize = MaterialTheme.typography.displaySmall.fontSize)
    }
}

@Composable
fun RowScope.BackspaceButton(modifier: Modifier = Modifier, processInput: (String) -> Unit) {
    FilledTonalButton(onClick = { processInput("BS") }, modifier = modifier.weight(1f).aspectRatio(1f)) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace"
            )
    }
}

@Composable
fun RowScope.WeightedNumButton(symbol: String, modifier: Modifier = Modifier,
                               fontSize: TextUnit = MaterialTheme.typography.displaySmall.fontSize,
                               processInput: (String) -> Unit
                               ) {
    FilledTonalButton(onClick = { processInput(symbol) }, modifier = modifier.aspectRatio(1f).weight(1f), shape = CircleShape) {
        ButtonText(symbol, modifier, fontSize)
    }
}

@Composable
fun RowScope.WeightedFuncButton(symbol: String, modifier: Modifier = Modifier,
                                fontSize: TextUnit = MaterialTheme.typography.displaySmall.fontSize,
                                processInput: (String) -> Unit
                                ) {
    FilledTonalButton(onClick = { processInput(symbol) }, modifier = modifier.aspectRatio(1f).weight(1f), shape = CircleShape) {
        ButtonText(symbol, modifier, fontSize)
    }
}

@Composable
fun ButtonText(innerText : String, modifier: Modifier = Modifier,
               fontSize: TextUnit) {
    Text(innerText, fontSize = fontSize)
}

@Preview
@Composable
fun ComposablePreview() {
    Main(Modifier)
}