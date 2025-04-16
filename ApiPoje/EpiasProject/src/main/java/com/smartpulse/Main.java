package com.smartpulse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Scanner;


public class Main {
	

	public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Kullanıcı adınızı girin: ");
        String username = scanner.nextLine();

        System.out.print("Şifrenizi girin: ");
        String password = scanner.nextLine();

        EpiasClient client = new EpiasClient(username, password);

        List<TransactionHistoryGipDataDto> transactions;
        try {
            transactions = client.getTransactionData();
            System.out.println();
            System.out.println("TGT Token: " + client.getCachedTGT()); // TGT ekrana yazılıyor
            System.out.println();
            System.out.println("---------------------------------------------------------------------------------------------------------");
        } catch (Exception e) {
            System.out.println("Kullanıcı bulunamıyor.");
            return;
        }

        // Verileri contractName’e göre grupla
        Map<String, List<TransactionHistoryGipDataDto>> grouped = new HashMap<>();
        for (TransactionHistoryGipDataDto dto : transactions) {
            grouped.computeIfAbsent(dto.getContractName(), k -> new ArrayList<>()).add(dto);
        }

        // Tablo başlığı
        System.out.printf("%-25s %-25s %-25s %-25s %-25s%n",
                "Contract", "Tarih", "Toplam İşlem Tutarı", "Toplam İşlem Miktarı", "Ağırlıklı Ortalama Fiyatı");

        for (Map.Entry<String, List<TransactionHistoryGipDataDto>> entry : grouped.entrySet()) {
            String contract = entry.getKey();
            List<TransactionHistoryGipDataDto> list = entry.getValue();

            double totalAmount = 0;
            double totalQuantity = 0;

            for (TransactionHistoryGipDataDto dto : list) {
                totalAmount += (dto.getPrice() * dto.getQuantity()) / 10;
                totalQuantity += dto.getQuantity() / 10;
            }

            double avgPrice = totalQuantity != 0 ? totalAmount / totalQuantity : 0;
            LocalDateTime dateTime = parseContractToDate(contract);
            String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyyHH:mm"));

            System.out.printf("%-15s %-20s %-20.2f %-20.2f %-20.2f%n",
                    contract, formattedDate, totalQuantity, totalAmount, avgPrice);
        }
    }

    private static LocalDateTime parseContractToDate(String contract) {
        String datePart = contract.substring(2, 10); // YYAAGGSS
        int year = 2000 + Integer.parseInt(datePart.substring(0, 2));
        int month = Integer.parseInt(datePart.substring(2, 4));
        int day = Integer.parseInt(datePart.substring(4, 6));
        int hour = Integer.parseInt(datePart.substring(6, 8));	
        return LocalDateTime.of(year, month, day, hour, 0);
    }

}
