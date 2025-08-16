fun main() {
    val row = 50
    val col = 270
    val universe = Universe(row, col)
    (0 until 2000).forEach { _ ->
        universe.setLiveCellAt((0..<row).random(), (0..<col).random())
    }
    while (true) {
        clearConsole()
        print("\b")
        println(universe.grid)
        universe.createNextGeneration()
        Thread.sleep(2000L)
    }
}

fun clearConsole() {
    try {
        val os = System.getProperty("os.name")
        val pb: ProcessBuilder
        if (os.contains("Windows")) {
            pb = ProcessBuilder("cmd", "/c", "cls")
        } else {
            pb = ProcessBuilder("clear")
        }
        pb.inheritIO().start().waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

val eol by lazy { System.getProperty("line.separator") }

enum class Cell {
    Live,
    Dead,
}

fun Char.toCell() =
    when (this) {
        '*' -> Cell.Live
        '.' -> Cell.Dead
        else -> error("Unknown char $this")
    }

fun Cell.toChar() =
    when (this) {
        Cell.Live -> '*'
        Cell.Dead -> '.'
    }

data class NDCells(
    val count: Int,
) : Iterable<Cell> {
    private val elements: Array<Cell> = Array(count) { Cell.Dead }

    companion object {
        fun from(elements: Array<Cell>) =
            elements.foldIndexed(NDCells(elements.size)) { index, acc, cell ->
                acc.apply {
                    set(index, cell)
                }
            }
    }

    operator fun get(index: Int) = elements[index]

    operator fun set(
        index: Int,
        cell: Cell,
    ) {
        elements[index] = cell
    }

    override fun iterator() = elements.iterator()

    val size: Int
        get() = elements.size

    fun clone() =
        elements.foldIndexed(NDCells(size)) { index, acc, cell ->
            acc.apply {
                set(index, cell)
            }
        }
}

fun String.toNDCells() = NDCells.from(map { it.toCell() }.toTypedArray())

data class NDCells2D(
    val rows: Int,
    val columns: Int,
) : Iterable<NDCells> {
    private val data: Array<NDCells> = Array(rows) { NDCells(columns) }

    companion object {
        fun from(cells: Array<NDCells>): NDCells2D {
            val rows = cells.size
            val columns =
                if (rows == 0) {
                    0
                } else {
                    cells[0].count
                }

            require(cells.all { it.count == columns }) { "All cells should be the same number of columns" }

            return cells.foldIndexed(NDCells2D(rows, columns)) { index, acc, column ->
                acc.apply {
                    acc.data[index] = column
                }
            }
        }
    }

    operator fun get(
        row: Int,
        column: Int,
    ) = data[row][column]

    operator fun set(
        row: Int,
        column: Int,
        cell: Cell,
    ) {
        data[row][column] = cell
    }

    override fun iterator() = data.iterator()

    fun clone() =
        data.foldIndexed(NDCells2D(rows, columns)) { index, acc, column ->
            acc.apply {
                acc.data[index] = column.clone()
            }
        }
}

class Board(
    rows: Int = DEFAULT_ROW_COUNT,
    columns: Int = DEFAULT_COLUMN_COUNT,
) {
    private var cells = anArrayOfDeadCells(rows, columns)

    companion object {
        const val DEFAULT_ROW_COUNT = 3
        const val DEFAULT_COLUMN_COUNT = 3

        fun loadFrom(gridContents: String): Board {
            val cells =
                NDCells2D
                    .from(
                        splitIntoRows(gridContents)
                            .map(::splitIntoCell)
                            .toTypedArray(),
                    )
            return Board(cells.rows, cells.columns).apply {
                this.cells = cells
            }
        }

        fun loadFrom(data: Array<NDCells>): Board {
            val cells =
                NDCells2D
                    .from(data)
            return Board(cells.rows, cells.columns).apply {
                this.cells = cells
            }
        }

        private fun splitIntoCell(row: String) = row.toNDCells()

        private fun splitIntoRows(gridContents: String) = gridContents.split(eol)
    }

    private fun anArrayOfDeadCells(
        rows: Int,
        columns: Int,
    ) = NDCells2D(rows, columns)

    fun convertToString() =
        cells
            .map { cell -> cell.map(Cell::toChar).toCharArray() }
            .fold(StringBuffer()) { acc, chars ->
                acc.append(chars).apply {
                    if (!chars.isEmpty()) {
                        acc.append(eol)
                    }
                }
            }.toString()

    fun getLiveNeighboursAt(
        x: Int,
        y: Int,
    ): Int {
        var liveNeighbourCount = 0
        for (xPosition in (x - 1)..x + 1) {
            for (yPosition in (y - 1)..y + 1) {
                if (!cellIsCentralCell(xPosition, yPosition, x, y)) { // Cell does not count itself as a neighbour
                    liveNeighbourCount += countLiveNeighboursInCell(xPosition, yPosition) // Increment counter if LIVE
                }
            }
        }
        return liveNeighbourCount
    }

    private fun countLiveNeighboursInCell(
        x: Int,
        y: Int,
    ): Int {
        if (cellIsOutsideBorders(x, y)) {
            return 0
        }
        if (cells[x, y] == Cell.Live) {
            return 1
        } else {
            return 0
        }
    }

    private fun cellIsCentralCell(
        x: Int,
        y: Int,
        centralX: Int,
        centralY: Int,
    ) = (x == centralX && y == centralY)

    private fun cellIsOutsideBorders(
        x: Int,
        y: Int,
    ) = (y < 0 || y >= cells.columns) || (x < 0 || x >= cells.rows)

    operator fun get(
        x: Int,
        y: Int,
    ) = cells[x, y]

    operator fun set(
        x: Int,
        y: Int,
        cell: Cell,
    ) {
        cells[x, y] = cell
    }

    fun getWidth() = cells.columns

    fun getHeight() = cells.rows

    fun getContents() = cells.clone()

    override fun toString() = convertToString()
}

class Universe(
    rows: Int,
    columns: Int,
) {
    private var currentBoardContent = Board(rows, columns)

    companion object {
        fun loadFrom(initialGridContents: String): Universe {
            val board = Board.loadFrom(initialGridContents)
            return Universe(board.getHeight(), board.getWidth()).apply {
                currentBoardContent = board
            }
        }

        fun seededWith(gridContents: String) = gridContents
    }

    fun spawnsANewGeneration() = createNextGeneration()

    fun createNextGeneration() {
        val nextGenerationContent = StringBuffer()
        val maxColumn = currentBoardContent.getWidth()
        val maxRow = currentBoardContent.getHeight()

        for (row in 0..<maxRow) { // From top to bottom
            for (column in 0..<maxColumn) { // From left to right
                val currentCell = currentBoardContent[row, column]
                val neighbourCount: Int =
                    currentBoardContent.getLiveNeighboursAt(row, column) // getLiveNeighboursAt() defined in Grid.java
                var nextCell: Cell

                // Based on the rules of game-of-life, calculate next state
                if (currentCell == Cell.Live) { // If cell is currently LIVE
                    if ((neighbourCount == 2) || (neighbourCount == 3)) {
                        nextCell = Cell.Live // Stay LIVE if 2 or 3 neighbours are LIVE
                    } else {
                        nextCell = Cell.Dead // Else, become DEAD due to underpopulation or overcrowding
                    }
                } else { // If cell is currently DEAD
                    if (neighbourCount == 3) {
                        nextCell = Cell.Live // Become LIVE if 3 neighbours are LIVE
                    } else {
                        nextCell = Cell.Dead // Else, stay DEAD
                    }
                }
                nextGenerationContent.append(nextCell.toChar())
            }
            if (row < maxRow - 1) {
                nextGenerationContent.append(eol)
            }
        }

        currentBoardContent = Board.loadFrom(nextGenerationContent.toString())
    }

    val grid: String
        get() = currentBoardContent.toString()

    val cells: NDCells2D
        get() = currentBoardContent.getContents()

    fun setLiveCellAt(
        row: Int,
        column: Int,
    ) {
        currentBoardContent[row, column] = Cell.Live
    }

    fun setDeadCellAt(
        row: Int,
        column: Int,
    ) {
        currentBoardContent[row, column] = Cell.Dead
    }

    fun getCellAt(
        row: Int,
        column: Int,
    ) = currentBoardContent[row, column]
}
