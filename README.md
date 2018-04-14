# console-downloader

#### Requirements
Java 1.8

#### Build and package
```
./gradlew jar
```
#### Run tests
```
./gradlew test
```

#### Usage 
Run from project directory
```
java -jar build/libs/console-downloader-0.1-SNAPSHOT.jar [OPTION]...
```

Option | Description |
-------| ----------- |
-h , --help | Print help information
-f, --links-file-path=LINKS_FILE_PATH  | Path to file with links list. Valid file's line format is \<HTTP\|HTTPS link\>whitespace\<file name\>
-l, --speed-limit=SPEED_LIMIT | Download speed limit for all threads. Speed value can be used with suffixes - k,m to set speed in kilobytes or megabytes respectively. If suffix is not passed, value treated as speed in bytes. Speed can be passed as integer or as double
-n, --threads-number=THREADS_NUMBER | Number of threads for concurrent downloading
-o,--output-dir-path=OUTPUT_DIR_PATH | Path to dir where downloaded files will be saved
-i,--interactive-mode | If passed, instead of exit with error, app shows continue dialog when there are invalid rows in links file. If file contains more than 100 rows, invalid rows' numbers will be written to ~/console-downloader.invalid-rows-report


large files with links can be generated throught https://github.com/eug-filippov-mn/page-parser
