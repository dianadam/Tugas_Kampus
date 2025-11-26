// tugas2.java
public class tugas2 {
    // contoh data (minimal 8 elemen)
    static int[] data = {34, 7, 23, 32, 5, 62, 78, 12, 10};
    public static void main(String[] args) {
        System.out.print("Sebelum: ");
        printOutArray(data);

        long t0 = System.nanoTime();
        merge_Sort(data, 0, data.length - 1);
        long t1 = System.nanoTime();

        System.out.print("Sesudah (desc): ");
        printOutArray(data);

        System.out.println("Waktu eksekusi (ns): " + (t1 - t0));
    }

    // Merge sort (rekursif) yang menghasilkan urutan descending
    static void merge_Sort(int[] arr, int left, int right) {
        if (left >= right) return;
        int mid = left + (right - left) / 2;
        merge_Sort(arr, left, mid);
        merge_Sort(arr, mid + 1, right);
        mergeDesc(arr, left, mid, right);
    }

    // Merge dua subarray arr[left..mid] dan arr[mid+1..right] menjadi descending
    static void mergeDesc(int[] arr, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;

        // membuat temporary arrays
        int[] L = new int[n1];
        int[] R = new int[n2];

        for (int i = 0; i < n1; i++) L[i] = arr[left + i];
        for (int j = 0; j < n2; j++) R[j] = arr[mid + 1 + j];

        int i = 0, j = 0, k = left;
        // Karena mau descending: ambil nilai lebih besar lebih dulu
        while (i < n1 && j < n2) {
            if (L[i] >= R[j]) {
                arr[k++] = L[i++];
            } else {
                arr[k++] = R[j++];
            }
        }
        while (i < n1) arr[k++] = L[i++];
        while (j < n2) arr[k++] = R[j++];
    }

    static void printOutArray(int[] a) {
        System.out.print("[");
        for (int i = 0; i < a.length; i++) {
            System.out.print(a[i]);
            if (i < a.length - 1) System.out.print(", ");
        }
        System.out.println("]");
    }
}
