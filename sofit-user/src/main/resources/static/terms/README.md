# 약관 PDF 저장 디렉토리

이 디렉토리에 약관 PDF 파일을 업로드하면 Spring Boot 정적 리소스 핸들러가 자동으로 서빙합니다.

## 접근 URL

```
http://localhost:8080/terms/{filename}.pdf
```

## 파일 네이밍 컨벤션

```
{term_type_lowercase}_{version}.pdf

예시:
- personal_info_v1.0.pdf
- mydata_v1.0.pdf
- mybizdata_v1.0.pdf
- loan_application_v1.0.pdf
- loan_agreement_v1.0.pdf
```

## DB 저장 형식

`term.file_url` 컬럼에는 **상대 경로**만 저장합니다:

```
/terms/loan_application_v1.0.pdf
```

응답 시 `sofit.storage.base-url` 설정값과 조립되어 전체 URL로 반환됩니다.

## 향후 AWS 이전

S3로 이관할 때는:
1. 이 디렉토리의 PDF를 S3 버킷에 그대로 업로드
2. `application.yml`의 `sofit.storage.base-url`을 S3 도메인으로 교체
3. DB 데이터는 변경 불필요
