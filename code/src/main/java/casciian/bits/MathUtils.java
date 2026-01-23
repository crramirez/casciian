/*
 * Casciian - Java Text User Interface
 *
 * Written 2013-2025 by Autumn Lamonte
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along
 * with this software. If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package casciian.bits;

/**
 * MathUtils contains miscellaneous mathematical computations.
 *
 * <p> The eigenvector functions are ultimately based on the public-domain
 * JAMA code.  These specific ones were made available by Connelly Barnes at:
 * http://barnesc.blogspot.com/2007/02/eigenvectors-of-3x3-symmetric-matrix.html
 * </p>
 */
public class MathUtils {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor prevents accidental creation of this class.
     */
    private MathUtils() {}

    // ------------------------------------------------------------------------
    // MathUtils --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Compute 2D distance between x and y.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return the distance
     */
    private static double hypot2(double x, double y) {
        return Math.sqrt(x*x + y*y);
    }

    /**
     * Symmetric Householder reduction to tridiagonal form.
     *
     * @param V input/output: eigenvectors matrix (modified in-place)
     * @param d input/output: diagonal elements of the tridiagonal matrix (modified in-place)
     * @param e output: off-diagonal elements of the tridiagonal matrix
     */
    private static void tred2(double V[][], double d[], double e[]) {

        //  This is derived from the Algol procedures tred2 by Bowdler,
        //  Martin, Reinsch, and Wilkinson, Handbook for Auto. Comp.,
        //  Vol.ii-Linear Algebra, and the corresponding Fortran subroutine
        //  in EISPACK.
        for (int j = 0; j < 3; j++) {
            d[j] = V[3-1][j];
        }

        // Householder reduction to tridiagonal form.

        for (int i = 3-1; i > 0; i--) {

            // Scale to avoid under/overflow.

            double scale = 0.0;
            double h = 0.0;
            for (int k = 0; k < i; k++) {
                scale = scale + Math.abs(d[k]);
            }
            if (scale == 0.0) {
                e[i] = d[i-1];
                for (int j = 0; j < i; j++) {
                    d[j] = V[i-1][j];
                    V[i][j] = 0.0;
                    V[j][i] = 0.0;
                }
            } else {

                // Generate Householder vector.

                for (int k = 0; k < i; k++) {
                    d[k] /= scale;
                    h += d[k] * d[k];
                }
                double f = d[i-1];
                double g = Math.sqrt(h);
                if (f > 0) {
                    g = -g;
                }
                e[i] = scale * g;
                h = h - f * g;
                d[i-1] = f - g;
                for (int j = 0; j < i; j++) {
                    e[j] = 0.0;
                }

                // Apply similarity transformation to remaining columns.

                for (int j = 0; j < i; j++) {
                    f = d[j];
                    V[j][i] = f;
                    g = e[j] + V[j][j] * f;
                    for (int k = j+1; k <= i-1; k++) {
                        g += V[k][j] * d[k];
                        e[k] += V[k][j] * f;
                    }
                    e[j] = g;
                }
                f = 0.0;
                for (int j = 0; j < i; j++) {
                    e[j] /= h;
                    f += e[j] * d[j];
                }
                double hh = f / (h + h);
                for (int j = 0; j < i; j++) {
                    e[j] -= hh * d[j];
                }
                for (int j = 0; j < i; j++) {
                    f = d[j];
                    g = e[j];
                    for (int k = j; k <= i-1; k++) {
                        V[k][j] -= (f * e[k] + g * d[k]);
                    }
                    d[j] = V[i-1][j];
                    V[i][j] = 0.0;
                }
            }
            d[i] = h;
        }

        // Accumulate transformations.

        for (int i = 0; i < 3-1; i++) {
            V[3-1][i] = V[i][i];
            V[i][i] = 1.0;
            double h = d[i+1];
            if (h != 0.0) {
                for (int k = 0; k <= i; k++) {
                    d[k] = V[k][i+1] / h;
                }
                for (int j = 0; j <= i; j++) {
                    double g = 0.0;
                    for (int k = 0; k <= i; k++) {
                        g += V[k][i+1] * V[k][j];
                    }
                    for (int k = 0; k <= i; k++) {
                        V[k][j] -= g * d[k];
                    }
                }
            }
            for (int k = 0; k <= i; k++) {
                V[k][i+1] = 0.0;
            }
        }
        for (int j = 0; j < 3; j++) {
            d[j] = V[3-1][j];
            V[3-1][j] = 0.0;
        }
        V[3-1][3-1] = 1.0;
        e[0] = 0.0;
    }

    /**
     * Symmetric tridiagonal QL algorithm.
     *
     * @param V input/output: eigenvectors matrix (modified in-place)
     * @param d input/output: diagonal elements of the tridiagonal matrix (modified in-place, becomes eigenvalues)
     * @param e input: off-diagonal elements of the tridiagonal matrix
     */
    private static void tql2(double V[][], double d[], double e[]) {

        //  This is derived from the Algol procedures tql2, by Bowdler,
        //  Martin, Reinsch, and Wilkinson, Handbook for Auto. Comp.,
        //  Vol.ii-Linear Algebra, and the corresponding Fortran subroutine
        //  in EISPACK.

        for (int i = 1; i < 3; i++) {
            e[i-1] = e[i];
        }
        e[3-1] = 0.0;

        double f = 0.0;
        double tst1 = 0.0;
        final double eps = Math.pow(2.0, -52.0);
        for (int l = 0; l < 3; l++) {

            // Find small subdiagonal element

            tst1 = Math.max(tst1, Math.abs(d[l]) + Math.abs(e[l]));
            int m = l;
            while (m < 3) {
                if (Math.abs(e[m]) <= eps*tst1) {
                    break;
                }
                m++;
            }

            // If m == l, d[l] is an eigenvalue,
            // otherwise, iterate.

            if (m > l) {
                int iter = 0;
                do {
                    iter = iter + 1;  // (Could check iteration count here.)

                    // Compute implicit shift

                    double g = d[l];
                    double p = (d[l+1] - g) / (2.0 * e[l]);
                    double r = hypot2(p,1.0);
                    if (p < 0) {
                        r = -r;
                    }
                    d[l] = e[l] / (p + r);
                    d[l+1] = e[l] * (p + r);
                    double dl1 = d[l+1];
                    double h = g - d[l];
                    for (int i = l+2; i < 3; i++) {
                        d[i] -= h;
                    }
                    f = f + h;

                    // Implicit QL transformation.

                    p = d[m];
                    double c = 1.0;
                    double c2 = c;
                    double c3 = c;
                    double el1 = e[l+1];
                    double s = 0.0;
                    double s2 = 0.0;
                    for (int i = m-1; i >= l; i--) {
                        c3 = c2;
                        c2 = c;
                        s2 = s;
                        g = c * e[i];
                        h = c * p;
                        r = hypot2(p,e[i]);
                        e[i+1] = s * r;
                        s = e[i] / r;
                        c = p / r;
                        p = c * d[i] - s * g;
                        d[i+1] = h + s * (c * g + s * d[i]);

                        // Accumulate transformation.

                        for (int k = 0; k < 3; k++) {
                            h = V[k][i+1];
                            V[k][i+1] = s * V[k][i] + c * h;
                            V[k][i] = c * V[k][i] - s * h;
                        }
                    }
                    p = -s * s2 * c3 * el1 * e[l] / dl1;
                    e[l] = s * p;
                    d[l] = c * p;

                    // Check for convergence.

                } while (Math.abs(e[l]) > eps*tst1);
            }
            d[l] = d[l] + f;
            e[l] = 0.0;
        }

        // Sort eigenvalues and corresponding vectors.

        for (int i = 0; i < 3 - 1; i++) {
            int k = i;
            double p = d[i];
            for (int j = i+1; j < 3; j++) {
                if (d[j] < p) {
                    k = j;
                    p = d[j];
                }
            }
            if (k != i) {
                d[k] = d[i];
                d[i] = p;
                for (int j = 0; j < 3; j++) {
                    p = V[j][i];
                    V[j][i] = V[j][k];
                    V[j][k] = p;
                }
            }
        }
    }

    /**
     * Compute the eigenvalues and eigenvectors of a <em>symmetric</em> 3x3
     * matrix.  Coordinates for the matrices are as A[row][col] and
     * V[row][col].
     *
     * @param A input: the 3x3 matrix.  It <em>must</em> be symmetric.
     * @param V output: the eigenvectors.  Each column is a vector.  The
     * first column (V[0][0], V[1][0], V[2][0]) will correspond to the first
     * eigenvector (d[0]), the second column (V[0][1], V[1][1], V[2][1]) will
     * correspond to the second eigenvector (d[1]), and the third column
     * (V[0][2], V[1][2], V[2][2]) will correspond to the third eigenvector
     * (d[2]).
     * @param d output: the eigenvalues.  These will be sorted smallest to
     * largest.
     */
    public static void eigen3(double A[][], double V[][], double d[]) {
        double [] e = new double[3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                V[i][j] = A[i][j];
            }
        }
        tred2(V, d, e);
        tql2(V, d, e);
    }

    /**
     * Test the eigenvector code.
     *
     * @param args command line arguments
     */
    public static void main(final String [] args) {
        double [][] A = new double[3][3];
        double [][] V = new double[3][3];
        double [] d = new double[3];
        /*
         * For the matrix:
         *
         *     -26 -33 -25
         *     -33  42  23
         *     -25  23  -4
         *
         * The eigenvalues are:
         *
         *   -45.8631
         *   -10.8460
         *    68.7091
         *
         * The corresponding eigenvectors are:
         *
         *  -0.8859   -0.2450    0.3937
         *  -0.2268   -0.5114   -0.8288
         *  -0.4044    0.8236   -0.3975
         *
         * Note that the coordinates for this EISPACK code reports the
         * result like V[row][col].
         */
        A[0][0] = -26;
        A[1][0] = -33;
        A[2][0] = -25;
        A[0][1] = -33;
        A[1][1] = 42;
        A[2][1] = 23;
        A[0][2] = -25;
        A[1][2] = 23;
        A[2][2] = -4;

        System.out.printf("A: [ %8.4f %8.4f %8.4f]\n   [ %8.4f %8.4f %8.4f]\n   [ %8.4f %8.4f %8.4f]\n",
            A[0][0], A[0][1], A[0][2],
            A[1][0], A[1][1], A[1][2],
            A[2][0], A[2][1], A[2][2]
        );

        eigen3(A, V, d);

        System.out.printf("\neigenvalues: %8.4f %8.4f %8.4f\n\n",
            d[0], d[1], d[2]);

        System.out.printf("V: [ %8.4f %8.4f %8.4f]\n   [ %8.4f %8.4f %8.4f]\n   [ %8.4f %8.4f %8.4f]\n",
            V[0][0], V[0][1], V[0][2],
            V[1][0], V[1][1], V[1][2],
            V[2][0], V[2][1], V[2][2]
        );

    }

}
