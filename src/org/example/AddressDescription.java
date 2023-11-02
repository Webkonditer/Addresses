package org.example;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class AddressDescription {

    public static void main(String[] args) {

        // pattern1: task1 "2010-01-01" "1422396, 1450759, 1449192, 1451562"
        // pattern2: task2 проезд

        if (args[0].equals("task1") && args.length == 3) {
            task1(args);
        } else if (args[0].equals("task2") && args.length == 2) {
            task2(args);
        } else {
            System.out.println("Usage: java AddressDescription task1 <date> <object_ids> or java AddressDescription task2 <key>");
            return;
        }
    }

    private static void task1(String[] args) {

        // Получаем дату и идентификаторы объектов из аргументов командной строки
        String dateString = args[1];
        int[] objectIds = Arrays.stream(args[2].split(",\\s*"))
                .mapToInt(Integer::parseInt)
                .toArray();

        try {
            // Парсим строку даты в объект типа Date
            Date targetDate = parseDate(dateString);

            // Читаем данные об адресах из файла
            List<Address> addressList = xmlToAddressList();

            if (addressList != null && !addressList.isEmpty()) {

                // Фильтруем адреса по заданной дате и идентификаторам объектов
                List<Address> addressListByDate = getAddressByDate(addressList, targetDate, objectIds);

                // Выводим найденные адреса
                addressListByDate.forEach(System.out::println);

            }
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    private static void task2(String[] args) {
        String key = args[1];

        // Читаем данные об адресах из файла
        List<Address> addressList = xmlToAddressList();

        // Фильтруем, имеющие нужный ключ, активные и актуальные
        List<Address> addressListByKey = addressList.stream()
                .filter(a -> a.getTypeName().equals(key) &&
                        a.getActive() &&
                        a.getActual()
                )
                .toList();

        // Получаем мапу иерархий из файла
        Map<Integer, Integer> hierarchyMap = xmlToHierarchyMap();

        // Собираем полные адреса
        StringBuilder fullAddress = new StringBuilder();
        for (Address address : addressListByKey) {

            Integer id = 0;
            Address tempAdres = address;
            while (true) {
                fullAddress = new StringBuilder(tempAdres.toStringShort() + fullAddress);
                id = hierarchyMap.get(tempAdres.getObjectId());
                if (id == null || id == 0 ) break;
                tempAdres = getAddressById(addressList, id);
            }

            // Удаление запятой в конце полного адреса
            fullAddress.deleteCharAt(fullAddress.length() - 2);

            // Выводим полные адреса
            System.out.println(fullAddress);
            fullAddress = new StringBuilder();
        }
    }

    // Метод получения адреса по objectId
    private static Address getAddressById(List<Address> list, int id) {
        Optional<Address> parentAddress = list.stream()
                .filter(a -> id == a.getObjectId())
                .findFirst();

        return parentAddress.orElse(null);
    }


    // Метод для парсинга XML файла в список объектов Address
    public static Map<Integer, Integer> xmlToHierarchyMap() {
        try {

            Map<Integer, Integer> hierarchyMap = new HashMap<>();

            // Создаем фабрику для создания парсера XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Загружаем XML-файл
            Document document = builder.parse("data/AS_ADM_HIERARCHY.XML");

            // Получаем корневой элемент (ITEMS)
            Element rootElement = document.getDocumentElement();

            // Получаем список всех элементов ITEM внутри ITEMS
            NodeList objectList = rootElement.getElementsByTagName("ITEM");

            for (int i = 0; i < objectList.getLength(); i++) {
                Element object = (Element) objectList.item(i);

                // Создаем Map из пар OBJECTID - PARENTOBJID
                hierarchyMap.put(
                        Integer.parseInt(object.getAttribute("OBJECTID")),
                        Integer.parseInt(object.getAttribute("PARENTOBJID"))
                );
            }

            // Возвращаем Map иерархий
            return hierarchyMap;

        } catch (Exception e) {
            return null;
        }
    }

    // Метод для парсинга строки с датой в объект Data
    private static Date parseDate(String dateString) throws ParseException {
        // Создаем объект SimpleDateFormat для парсинга даты
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.parse(dateString);
    }


    // Метод для фильтрации адресов по дате и идентификаторам объектов
    private static List<Address> getAddressByDate(List<Address> addressList, Date targetDate, int[] objectIds) {
        return addressList.stream()
                .filter(a -> (targetDate.compareTo(a.getStartDate()) >= 0)
                        && (targetDate.compareTo(a.getEndDate()) <= 0)
                        && Arrays.stream(objectIds).anyMatch(id -> id == a.getObjectId()))
                .collect(Collectors.toList());
    }

    // Метод для парсинга XML файла в список объектов Address
    public static List<Address> xmlToAddressList() {
        try {

            List<Address> addressList = new ArrayList<>();

            // Создаем фабрику для создания парсера XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Загружаем XML-файл
            Document document = builder.parse("data/AS_ADDR_OBJ.XML");

            // Получаем корневой элемент (ADDRESSOBJECTS)
            Element rootElement = document.getDocumentElement();

            // Получаем список всех элементов OBJECT внутри ADDRESSOBJECTS
            NodeList objectList = rootElement.getElementsByTagName("OBJECT");

            for (int i = 0; i < objectList.getLength(); i++) {
                Element object = (Element) objectList.item(i);

                // Создаем объект Address на основе прочитанных данных
                Address address = new Address(
                        Integer.parseInt(object.getAttribute("OBJECTID")),
                        object.getAttribute("NAME"),
                        object.getAttribute("TYPENAME"),
                        parseDate(object.getAttribute("STARTDATE")),
                        parseDate(object.getAttribute("ENDDATE")),
                        object.getAttribute("ISACTUAL").equals("1"),
                        object.getAttribute("ISACTIVE").equals("1")

                );
                addressList.add(address);
            }

            // Возвращаем список  адресов
            return addressList;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

class Address {
    private final int objectId;
    private final String name;
    private final String typeName;
    private final Date startDate;
    private final Date endDate;
    private final Boolean isActive;
    private final Boolean isActual;

    // Конструктор для создания объекта Address
    Address(int objectId, String name, String typeName, Date startDate, Date endDate, Boolean isActive, Boolean isActual) {
        this.objectId = objectId;
        this.name = name;
        this.typeName = typeName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
        this.isActual = isActual;
    }

    public int getObjectId() {
        return objectId;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Boolean getActive() {
        return isActive;
    }

    public Boolean getActual() {
        return isActual;
    }

    // Переопределение метода toString() для вывода объекта Address
    @Override
    public String toString() {
        return objectId + ": " + typeName + " " + name;
    }

    public String toStringShort() {return typeName + " " + name + ", ";}
}





