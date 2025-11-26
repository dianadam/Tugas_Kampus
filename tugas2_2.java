// tugas2_2.java
public class tugas2_2 {
    // contoh data (minimal 8 elemen), non-negatif dan rentang kecil
    static int[] data = {34, 7, 23, 32, 5, 62, 78, 12, 10};

    public static void main(String[] args) {
        System.out.print("Sebelum: ");
        printArray(data);

        long t0 = System.nanoTime();
        int[] sorted = countingSortDescending(data);
        long t1 = System.nanoTime();

        System.out.print("Sesudah (desc): ");
        printArray(sorted);

        System.out.println("Waktu eksekusi (ns): " + (t1 - t0));
    }

    // Counting Sort yang mengembalikan array baru terurut descending
    static int[] countingSortDescending(int[] arr) {
        if (arr.length == 0) return arr;

        // cari nilai maksimum (asumsi semua >= 0)
        int max = arr[0];
        int min = arr[0];
        for (int v : arr) {
            if (v > max) max = v;
            if (v < min) min = v;
        }

        if (min < 0) {
            throw new IllegalArgumentException("Counting sort versi ini mengharuskan semua elemen >= 0.");
        }

        int k = max + 1; // ukuran count array
        int[] count = new int[k];

        // hitung frekuensi
        for (int v : arr) count[v]++;

        // bentuk array hasil descending:
        int[] output = new int[arr.length];
        int idx = 0;
        // iterasi dari nilai terbesar ke kecil
        for (int val = k - 1; val >= 0; val--) {
            while (count[val] > 0) {
                output[idx++] = val;
                count[val]--;
            }
        }
        return output;
    }

    static void printArray(int[] a) {
        System.out.print("[");
        for (int i = 0; i < a.length; i++) {
            System.out.print(a[i]);
            if (i < a.length - 1) System.out.print(", ");
        }
        System.out.println("]");
    }
}
