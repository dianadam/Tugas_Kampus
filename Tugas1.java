public class Tugas1 {
    public static void main(String[] args) {

        int StrukturBaris; //Soal1
		
        String KataBaru = "Deklarasi tipe data String"; //Soal2
		
        int[] empatAngka = {07, 10, 20, 23}; //Soal3
		
		//Array 1 dimensi []
		//Array 2 dimensi [] []
		//soal4
        String[][] Angka = {
            {"1", "3", "5"},
            {"14", "19", "20"},
            {"22", "27", "29"}
        };
        java.util.LinkedList<Integer> listAngka = new java.util.LinkedList<>(); //Soal5
        listAngka.add(22);
        listAngka.add(19);
        listAngka.add(44);
        listAngka.add(60);
        listAngka.add(72);
        
		System.out.println(KataBaru); //printah output ke console
        System.out.println("Array empatAngka: " + java.util.Arrays.toString(empatAngka)); // printah output ke console namun hanya memanggil data type Array
        System.out.println("Isi array dua dimensi Angka:");
        for (String[] baris : Angka) {
            System.out.println(java.util.Arrays.toString(baris));
        }
        System.out.println("Isi LinkedList listAngka: " + listAngka);
    }
}
