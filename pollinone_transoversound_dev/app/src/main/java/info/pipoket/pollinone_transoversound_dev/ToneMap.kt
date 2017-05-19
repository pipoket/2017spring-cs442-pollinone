package info.pipoket.pollinone_transoversound_dev

/**
 * Created by pipoket on 2017/05/19.
 */
object ToneMap {
    // Based on FFT with size 1024
    val High : HashMap<Int, String> = hashMapOf(
            1981 to "0",
            2110 to "1",
            2196 to "2",
            2325 to "3",
            2411 to "4",
            2540 to "5",
            2627 to "6",
            2756 to "7",
            2885 to "8",
            2971 to "9",
            3100 to "a",
            3186 to "b",
            3316 to "c",
            3402 to "d",
            3531 to "e",
            3660 to "f",
            3746 to "g",
            3962 to "0",
            4220 to "1",
            4435 to "2",
            4651 to "3",
            4866 to "4",
            5081 to "5",
            5297 to "6",
            5512 to "7",
            5770 to "8",
            5986 to "9",
            6201 to "a",
            6416 to "b",
            6632 to "c",
            6847 to "d",
            7105 to "e",
            7321 to "f",
            7536 to "g"
    )
    val revHigh: HashMap<String, Int> = hashMapOf (
            "0" to 1981,
            "1" to 2110,
            "2" to 2196,
            "3" to 2325,
            "4" to 2411,
            "5" to 2540,
            "6" to 2627,
            "7" to 2756,
            "8" to 2885,
            "9" to 2971,
            "a" to 3100,
            "b" to 3186,
            "c" to 3316,
            "d" to 3402,
            "e" to 3531,
            "f" to 3660,
            "g" to 3746
    )
    val revTrueHigh: HashMap<String, Int> = hashMapOf (
            "0" to 2000,
            "1" to 2112,
            "2" to 2222,
            "3" to 2330,
            "4" to 2444,
            "5" to 2556,
            "6" to 2666,
            "7" to 2770,
            "8" to 2888,
            "9" to 2996,
            "a" to 3110,
            "b" to 3222,
            "c" to 3330,
            "d" to 3444,
            "e" to 3558,
            "f" to 3666,
            "g" to 3770
    )

    // FIXME: Based on FFT with size 4096
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