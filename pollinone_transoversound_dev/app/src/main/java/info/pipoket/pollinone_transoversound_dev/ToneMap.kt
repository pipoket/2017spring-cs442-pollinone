package info.pipoket.pollinone_transoversound_dev

/**
 * Created by pipoket on 2017/05/19.
 */
object ToneMap {
    val High : HashMap<Int, String> = hashMapOf(
            1991 to "0",
            2110 to "1",
            2217 to "2",
            2325 to "3",
            2433 to "4",
            2551 to "5",
            2659 to "6",
            2767 to "7",
            2885 to "8",
            2993 to "9",
            3100 to "a",
            3219 to "b",
            3326 to "c",
            3434 to "d",
            3552 to "e",
            3660 to "f",
            3768 to "g"

    )
    val revHigh: HashMap<String, Int> = hashMapOf (
            "0" to 1991,
            "1" to 2110,
            "2" to 2217,
            "3" to 2325,
            "4" to 2433,
            "5" to 2551,
            "6" to 2659,
            "7" to 2767,
            "8" to 2885,
            "9" to 2993,
            "a" to 3100,
            "b" to 3219,
            "c" to 3326,
            "d" to 3434,
            "e" to 3552,
            "f" to 3660,
            "g" to 3768
    )


    val low: HashMap<Int, String> = hashMapOf(
            990 to "0",
            1108 to "1",
            1216 to "2",
            1324 to "3",
            1442 to "4",
            1550 to "5",
            1658 to "6",
            1776 to "7",
            1884 to "8",
            1991 to "9",
            2110 to "a",
            2217 to "b",
            2325 to "c",
            2433 to "d",
            2551 to "e",
            2659 to "f",
            2767 to "g"
    )
    val revLow: HashMap<String, Int> = hashMapOf (
            "0" to 990,
            "1" to 1108,
            "2" to 1216,
            "3" to 1324,
            "4" to 1442,
            "5" to 1550,
            "6" to 1658,
            "7" to 1776,
            "8" to 1884,
            "9" to 1991,
            "a" to 2110,
            "b" to 2217,
            "c" to 2325,
            "d" to 2433,
            "e" to 2551,
            "f" to 2659,
            "g" to 2767
    )
}