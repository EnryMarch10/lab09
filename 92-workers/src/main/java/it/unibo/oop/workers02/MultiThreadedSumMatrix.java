package it.unibo.oop.workers02;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Matrix sum with multiple threads of a matrix of doubles.
 */
public final class MultiThreadedSumMatrix implements SumMatrix {

    private final int nthread;

    /**
     * 
     * @param nthread
     *            no. of thread performing the sum.
     */
    public MultiThreadedSumMatrix(final int nthread) {
        if (nthread > 0) {
            this.nthread = nthread;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private interface ArgumentAssert {
        /**
         * 
         * @param <T>
         * @param arg the argument to check
         * @param predicate
         * @return the arg checked
         * @throws IllegalArgumentException if argument is in an unexpected condition
         */
        static <T> T verifyLegalArgument(T arg, Predicate<T> predicate) {
            if (!predicate.test(arg)) {
                throw new IllegalArgumentException();
            }
            return arg;
        }
        /**
         * 
         * @param <T>
         * @param arg the argument to check
         * @param predicate
         * @return the arg checked
         * @throws IllegalStateException if state is unexpected to exist
         */
        static <T> T verifyStateArgument(T arg, Predicate<T> predicate) {
            if (!predicate.test(arg)) {
                throw new IllegalStateException();
            }
            return arg;
        }
    }

    private static final class Worker extends Thread {
        private final double[][] matrix;
        private final MatrixRange range;
        private double res;

        /**
         * Build a new worker.
         * 
         * @param matrix
         *            the array to sum
         * @param range
         *            the range of action
         */
        Worker(final double[][] matrix, final MatrixRange range) {
            super();
            this.matrix = Objects.requireNonNull(matrix);
            this.range = Objects.requireNonNull(range);
        }

        @Override
        @SuppressWarnings("PMD") // println called for testing
        public void run() {
            System.out.println("Working from position: " + range.start + " to position: " + range.end);
            for (final MatrixPos pos: range) {
                final double[] tmp = matrix[pos.iRow];
                this.res += tmp[pos.iCol];
            }
        }

        /**
         * Returns the result of summing up the doubles within the matrix.
         * 
         * @return the sum of every element in the matrix
         */
        public double getResult() {
            return this.res;
        }
    }

    private enum Position {
        SAME,
        BEFORE,
        AFTER
    }

    private static final class MatrixPos implements Cloneable {
        //private static boolean set = true;
        private static int nRows;
        private static int nCols;
        private int iRow;
        private int iCol;

        MatrixPos(final int row, final int col) {
            //if (!set) {
                this.iRow = ArgumentAssert.verifyLegalArgument(row, (x) -> x >= 0);
                this.iCol = ArgumentAssert.verifyLegalArgument(col, (x) -> x >= 0);
            //} else {
                //throw new IllegalStateException();
            //}
        }

        public static void setData(final int rows, final int cols) {
            //if (set) {
                MatrixPos.nRows = ArgumentAssert.verifyLegalArgument(rows, (arg) -> arg > 0);
                MatrixPos.nCols = ArgumentAssert.verifyLegalArgument(cols, (arg) -> arg > 0);
                //set = false;
            //} else {
                //throw new IllegalStateException();
            //}
        }
        public int getRow() {
            return iRow;
        }
        public int getCol() {
            return iCol;
        }

        public MatrixPos increment() {
            return increment(1);
        }
        public MatrixPos increment(final int offset) {
            if (offset > 0) {
                int tmpOffset = offset;
                int tmp = tmpOffset / nCols;
                iRow += tmp;
                tmpOffset -= tmp * nCols;
                tmp = iRow;
                iRow += (tmpOffset + iCol) / nCols;
                if (iRow != tmp) {
                    // come se ci fossero le (...) a destra dell'=
                    tmpOffset -= nCols - iCol;
                    iCol = 0;
                }
                iCol += tmpOffset % nCols;
            } else {
                throw new IllegalArgumentException();
            }
            ArgumentAssert.verifyLegalArgument(iCol, (x) -> x < nCols);
            ArgumentAssert.verifyLegalArgument(iRow, (x) -> x < nRows);
            return this;
        }
        public static Position position(final MatrixPos first, final MatrixPos second) {
            Position result = Position.SAME;
            if (first.getRow() > second.getRow()) {
                result = Position.AFTER;
            } else if (first.getRow() < second.getRow()) {
                result = Position.BEFORE;
            } else if (first.getCol() > second.getCol()) {
                result = Position.AFTER;
            } else if (first.getCol() < second.getCol()) {
                result = Position.BEFORE;
            }
            return result;
        }

        @Override
        public MatrixPos clone() {
            MatrixPos pos = null;
            try {
                pos = (MatrixPos) super.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace(); // NOPMD
            }
            return pos;
        }
        @Override
        public boolean equals(final Object obj) {
            return !(obj instanceof MatrixPos) || iCol == ((MatrixPos) obj).iCol && iRow == ((MatrixPos) obj).iRow;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + iRow;
            result = prime * result + iCol;
            return result;
        }
        @Override
        public String toString() {
            return "MatrixPos [row=" + iRow + ", col=" + iCol + "]";
        }
    }

    private static final class MatrixRange implements Iterable<MatrixPos> {
        private final MatrixPos start;
        private final MatrixPos end;

        /**
         * Creates a range of matrix coordinates.
         * 
         * @param start starting position
         * @param end ending position
         */
        MatrixRange(final MatrixPos start, final MatrixPos end) {
            this.start = ArgumentAssert.verifyLegalArgument(start, (arg) -> MatrixPos.position(start, end) == Position.BEFORE);
            this.end = end;
        }

        @Override
        public Iterator<MatrixPos> iterator() {
            return new Iterator<MultiThreadedSumMatrix.MatrixPos>() {
                private boolean hasNext = true;
                private final MatrixPos first = MatrixRange.this.start.clone();
                private final MatrixPos last = MatrixRange.this.end.clone();

                @Override
                public MatrixPos next() {
                    if (!this.first.equals(last)) {
                        final MatrixPos tmpPos = this.first.clone();
                        this.first.increment();
                        return tmpPos;
                    } else if (this.hasNext) {
                        this.hasNext = false;
                        return this.last.clone();
                    } else {
                        throw new NoSuchElementException();
                    }
                }
                @Override
                public boolean hasNext() {
                    return hasNext;
                }
            };
        }
    }

    @Override
    public double sum(final double[][] matrix) {
        double sum = 0;
        if (Objects.requireNonNull(matrix).length > 0) {
            final int rows = matrix.length;
            final int cols = matrix[0].length;
            MatrixPos.setData(rows, cols);
            final int length = rows * cols;
            final int size = length / nthread;
            final List<Worker> workers = new ArrayList<>(nthread);
            final MatrixPos pos = new MatrixPos(0, 0);
            int i = 0;
            for (; i < length % nthread; i++) {
                workers.add(new Worker(matrix, new MatrixRange(pos.clone(), pos.clone().increment(size)))); // era size + 1
                pos.increment(size + 1);
            }
            for (; i < nthread - 1; i++) {
                workers.add(new Worker(matrix, new MatrixRange(pos.clone(), pos.clone().increment(size - 1))));
                pos.increment(size);
            }
            workers.add(new Worker(matrix, new MatrixRange(pos.clone(), pos.clone().increment(size - 1))));
            for (final Worker w: workers) {
                w.start();
            }
            for (final Worker w: workers) {
                try {
                    w.join();
                    sum += w.getResult();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return sum;
    } 
}
