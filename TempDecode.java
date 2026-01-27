import java.util.Base64;

public class TempDecode {
    public static void main(String[] args) {
        String payload = "eyJsaWNlbnNlS2V5IjoiVFJJQUwtVEVTVC0wMDEiLCJsaWNlbnNlVHlwZSI6IlRSSUFMIiwiZGV2aWNlSWQiOiJhMWIyYzNkNGU1ZjZnN2g4aTlqMGsxbDJtM240bzVwNiIsImFjdGl2YXRpb25EYXRlIjoiMjAyNS0xMi0xOCIsImV4cGlyZURhdGUiOiIyMDI2LTAxLTAyIiwidmFsaWREYXlzIjoxNSwiZmVhdHVyZXMiOlsiYWxsIl19";
        System.out.println(new String(Base64.getDecoder().decode(payload)));
    }
}




























