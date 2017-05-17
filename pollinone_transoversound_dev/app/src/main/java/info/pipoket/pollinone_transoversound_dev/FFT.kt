package info.pipoket.pollinone_transoversound_dev

/******************************************************************************
 * Compilation:  javac FFT.java
 * Execution:    java FFT n
 * Dependencies: Complex.java

 * Compute the FFT and inverse FFT of a length n complex sequence.
 * Bare bones implementation that runs in O(n log n) time. Our goal
 * is to optimize the clarity of the code, rather than performance.

 * Limitations
 * -----------
 * -  assumes n is a power of 2

 * -  not the most memory efficient algorithm (because it uses
 * an object type for representing complex numbers and because
 * it re-allocates memory for the subarray, instead of doing
 * in-place or reusing a single temporary array)

 */

object FFT {

    // compute the FFT of x[], assuming its length is a power of 2
    fun fft(x: ArrayList<Complex>): ArrayList<Complex> {
        val n = x.size

        // base case
        if (n == 1) {
            val ret = ArrayList<Complex>(1)
            ret[0] = x[0]
            return ret
        }

        // radix 2 Cooley-Tukey FFT
        if (n % 2 != 0) {
            throw RuntimeException("n is not a power of 2")
        }

        // fft of even terms
        val even = ArrayList<Complex>(n / 2)
        for (k in 0..n / 2 - 1) {
            even[k] = x[2 * k]
        }
        val q = fft(even)

        // fft of odd terms
        val odd = even  // reuse the array
        for (k in 0..n / 2 - 1) {
            odd[k] = x[2 * k + 1]
        }
        val r = fft(odd)

        // combine
        val y = ArrayList<Complex>(n)
        for (k in 0..n / 2 - 1) {
            val kth = -2.0 * k.toDouble() * Math.PI / n
            val wk = Complex(Math.cos(kth), Math.sin(kth))
            y[k] = q[k].plus(wk.times(r[k]))
            y[k + n / 2] = q[k].minus(wk.times(r[k]))
        }
        return y
    }


    // compute the inverse FFT of x[], assuming its length is a power of 2
    fun ifft(x: ArrayList<Complex>): ArrayList<Complex> {
        val n = x.size
        var y = ArrayList<Complex>(n)

        // take conjugate
        for (i in 0..n - 1) {
            y[i] = x[i].conjugate()
        }

        // compute forward FFT
        y = fft(y)

        // take conjugate again
        for (i in 0..n - 1) {
            y[i] = y[i].conjugate()
        }

        // divide by n
        for (i in 0..n - 1) {
            y[i] = y[i].scale(1.0 / n)
        }

        return y

    }

    // compute the circular convolution of x and y
    fun cconvolve(x: ArrayList<Complex>, y: ArrayList<Complex>): ArrayList<Complex> {

        // should probably pad x and y with 0s so that they have same length
        // and are powers of 2
        if (x.size != y.size) {
            throw RuntimeException("Dimensions don't agree")
        }

        val n = x.size

        // compute FFT of each sequence
        val a = fft(x)
        val b = fft(y)

        // point-wise multiply
        val c = ArrayList<Complex>(n)
        for (i in 0..n - 1) {
            c[i] = a[i].times(b[i])
        }

        // compute inverse FFT
        return ifft(c)
    }


    // compute the linear convolution of x and y
    fun convolve(x: ArrayList<Complex>, y: ArrayList<Complex>): ArrayList<Complex> {
        val ZERO = Complex(0.0, 0.0)

        val a = ArrayList<Complex>(2 * x.size)
        for (i in x.indices) a[i] = x[i]
        for (i in x.size..2 * x.size - 1) a[i] = ZERO

        val b = ArrayList<Complex>(2 * y.size)
        for (i in y.indices) b[i] = y[i]
        for (i in y.size..2 * y.size - 1) b[i] = ZERO

        return cconvolve(a, b)
    }
}