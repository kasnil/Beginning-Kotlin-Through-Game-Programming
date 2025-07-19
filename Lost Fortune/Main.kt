const val GOLD_PIECES = 900

fun main() {
    // get the information
    println("Welcome to Lost Fortune\n")
    println("Please enter the following for your personalized adventure")

    print("Enter a number: ")
    val adventurers = readLine()?.trim()?.toIntOrNull() ?: 0

    print("Enter a number, smaller than the first: ")
    val killed = readLine()?.trim()?.toIntOrNull() ?: 0

    val survivors = adventurers - killed

    print("Enter your last name: ")
    val leader = readLine()?.trim() ?: ""

    // tell the story
    print("\nA brave group of $adventurers set out on a quest ")
    print("-- in search of the lost treasure of the Ancient Dwarves. ")
    println("The group was led by that legendary rogue, $leader.")
	
    print("\nAlong the way, a band of marauding ogres ambushed the party. ")
    print("All fought bravely under the command of $leader")
    print(", and the ogres were defeated, but at a cost. ")
    print("Of the adventurers, $killed were vanquished, ")
    println("leaving just $survivors in the group.")
	
    print("\nThe party was about to give up all hope. ")
    print("But while laying the deceased to rest, ")
    print("they stumbled upon the buried fortune. ")
    print("So the adventurers split $GOLD_PIECES gold pieces.")
    print("$leader held on to the extra ${GOLD_PIECES % survivors}")
    println(" pieces to keep things fair of course.")
}
