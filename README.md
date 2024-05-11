# Document Converter

Service to convert document using LibreOffice

## Build image

```bash
docker build -t document-converter .
```

## Run container

```bash
docker run --rm -p 8080:8080 document-converter
```

## Convert document

```bash
# using raw/binary
curl --request POST \
  --url 'http://localhost:8080/convert' \
  --header 'Content-Type:application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  --data "@path/to/document.docx" \
  --output document.pdf

# using json
curl --request POST \
  --url http://localhost:8080/convert \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --data '{ "file": "<document base64>" }'

# using multipart/form
curl --request POST \
  --url http://localhost:8080/convert \
  --header 'Content-Type: multipart/form-data' \
  --form file=@path/to/document.docx
```
