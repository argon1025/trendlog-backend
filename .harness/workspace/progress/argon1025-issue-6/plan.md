# 코드 스타일 자동 적용 (spotless + palantir-java-format)

- 대상 저장소: `trendlog-backend`
- 브랜치: `argon1025/issue-6`
- 관련 이슈: https://github.com/argon1025/trendlog-backend/issues/6 (제목 "코드스타일 적용", 본문 "spotless")

## Context

이 저장소에는 코드 스타일을 강제하는 장치가 하나도 없어요. 그래서 지금 Java 파일 14개의 들여쓰기와 import 순서는 각자 편집기가 만들어 준 모양(탭 들여쓰기, `java/javax → org → com → 나머지` 순 import)으로 남아 있고, 앞으로 사람이나 에이전트가 코드를 추가할 때마다 모양이 조금씩 갈라져요.

목표는 한 가지예요. 빌드를 돌리면 소스가 자동으로 한 가지 규칙으로 정리되고, PR 화면에서 정리된 코드만 보이게 만들어요.

사용자가 확정한 결정은 다음 두 개예요.
- 코드 스타일은 **palantir-java-format의 `PALANTIR` 스타일**을 써요(4칸 공백 들여쓰기, 120컬럼).
- 적용 시점은 **Maven `process-sources` 단계에 `spotless:apply`를 묶는 방식**이에요. `./mvnw compile`, `./mvnw test`, `./mvnw spring-boot:run` 중 무엇을 돌려도 소스가 정리돼요. git 훅 설치나 GitHub Actions 워크플로는 이번 범위에 넣지 않아요.
- 정리 대상은 **`src/main/java`와 `src/test/java`의 Java 파일만**이에요. `pom.xml` 요소 정렬(`sortPom`)과 yaml·markdown 공백 정리는 이번에 하지 않아요.

## 필수 선행 읽기

`.harness/docs/` 위키에는 아직 문서가 0개예요(`wiki_index.py` 출력이 헤더 한 줄뿐). 그래서 읽을 위키 문서가 없고, 대신 이 작업에 걸리는 기존 결정 두 개를 여기에 옮겨 적어요.

- `io.trendlog.api.external.kis` 패키지의 주석은 "무엇을 하는 코드인가"만 한두 줄로 적기로 사용자와 합의했어요. 이번 작업에서 어떤 파일에도 설명 주석을 새로 넣지 마세요. 출처는 `.harness/workspace/progress/argon1025-openapi/feedback.md`예요.
- 위 결정 때문에 포매터가 javadoc 본문을 건드리지 않게 둬요. palantir-java-format의 `formatJavadoc` 기본값이 `false`라서 별도 설정 없이 그대로 유지돼요.

## 적용할 도구 사실 (검증한 값)

- `com.diffplug.spotless:spotless-maven-plugin` 최신 버전은 **3.9.0**이에요(Maven Central `maven-metadata.xml` 기준, 2026-07-27 갱신). 이 플러그인은 Maven이 JRE 17 이상에서 돌아야 해요. 이 프로젝트는 `.sdkmanrc`에서 Java 25(`25.0.3-amzn`)를 쓰므로 조건을 만족해요.
- `com.palantir.javaformat:palantir-java-format` 최신 버전은 **2.96.0**이에요. spotless 3.9.0이 버전을 지정하지 않으면 `2.80.0`을 쓰므로, 재현성을 위해 pom에 명시적으로 적어요.
- spotless의 palantir 단계는 **코드 포매팅과 import 순서 정리, 사용하지 않는 import 제거를 한꺼번에** 해요(`PalantirJavaFormatStep` 주석 "Creates a step which formats everything - code, import order, and unused imports"). 그래서 `<removeUnusedImports/>`를 따로 넣지 않아요.
- 예전에 google-java-format이 JDK 16 이상에서 요구했던 `--add-exports` 우회 설정은 필요 없어요. spotless가 자체 클래스로더로 해결했어요(`CHANGES.md`의 "PalantirJavaFormatStep no longer needs the `--add-exports` calls" 항목).

## 이 변경이 만드는 diff의 성격

첫 `spotless:apply`는 Java 파일 14개를 전부 다시 씁니다. 세 가지가 동시에 바뀌어요.

1. 들여쓰기가 탭에서 4칸 공백으로 바뀌어요.
2. import가 한 덩어리로 합쳐지고 ASCII 순으로 정렬돼요. 지금은 `org.springframework` 묶음과 `io.trendlog` 묶음 사이에 빈 줄이 있는데, 그 빈 줄이 사라지고 `io.trendlog` → `java.time` → `lombok` → `org.junit` → `org.springframework` 순서가 돼요. `static` import는 계속 맨 위 별도 묶음에 남아요.
3. 120컬럼을 넘는 줄이 다시 줄바꿈돼요. 가장 긴 줄은 `KisAuthClientTest.java`의 161자예요.

이건 의도한 변경이라서, 리뷰할 때 "코드가 망가졌다"고 보지 마세요.

## 작업 파일

| 파일 | 작업 |
|---|---|
| `pom.xml` | `<properties>`에 버전 두 개 추가, `<build><plugins>`에 spotless 플러그인 블록 추가 |
| `.editorconfig` (새 파일) | 편집기가 Java 파일에 4칸 공백을 넣도록 맞춤 |
| `README.md` | 기술 스택 표에 포매터 한 줄 추가, "코드 스타일" 절 추가 |
| `src/main/java/**/*.java`, `src/test/java/**/*.java` (14개) | `./mvnw spotless:apply` 결과 반영 |
| `.harness/workspace/progress/argon1025-issue-6/feedback.md` | 아래 "기록할 피드백" 항목 append |

## 커밋 1 — spotless 설정 추가

`pom.xml`의 `<properties>`를 이렇게 만들어요. 이 파일은 탭 들여쓰기를 쓰니 탭을 그대로 유지하세요.

```xml
<properties>
	<java.version>25</java.version>
	<spotless-maven-plugin.version>3.9.0</spotless-maven-plugin.version>
	<palantir-java-format.version>2.96.0</palantir-java-format.version>
</properties>
```

`<build><plugins>` 안에서 기존 `maven-compiler-plugin` 블록 다음, `</plugins>` 앞에 아래 블록을 넣어요.

```xml
<plugin>
	<groupId>com.diffplug.spotless</groupId>
	<artifactId>spotless-maven-plugin</artifactId>
	<version>${spotless-maven-plugin.version}</version>
	<configuration>
		<java>
			<includes>
				<include>src/main/java/**/*.java</include>
				<include>src/test/java/**/*.java</include>
			</includes>
			<palantirJavaFormat>
				<version>${palantir-java-format.version}</version>
				<style>PALANTIR</style>
			</palantirJavaFormat>
		</java>
	</configuration>
	<executions>
		<execution>
			<id>spotless-apply</id>
			<phase>process-sources</phase>
			<goals>
				<goal>apply</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

`.editorconfig`를 저장소 루트에 새로 만들어요. Java 파일만 대상으로 삼아서 `pom.xml`의 탭 들여쓰기에는 영향을 주지 않아요.

```
root = true

[*.java]
indent_style = space
indent_size = 4
max_line_length = 120
trim_trailing_whitespace = true
insert_final_newline = true
```

`README.md`의 기술 스택 표 맨 아래에 한 줄을 더해요.

```
| 코드 포매터 | spotless 3.9.0 + palantir-java-format 2.96.0 (`PALANTIR` 스타일) |
```

그리고 "로컬 실행" 절 뒤에 아래 절을 넣어요.

```markdown
## 코드 스타일

Java 코드 서식은 spotless가 자동으로 맞춰요. 규칙은 palantir-java-format의 `PALANTIR` 스타일이고, 들여쓰기는 4칸 공백, 한 줄은 120컬럼까지예요. import 순서 정리와 쓰지 않는 import 제거도 함께 처리해요.

`spotless:apply`가 Maven `process-sources` 단계에 묶여 있어서, 아래 명령 중 무엇을 돌려도 소스가 정리돼요.

```bash
./mvnw compile
./mvnw test
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

서식이 맞는지만 확인하고 싶으면 다음 명령을 쓰세요. 어긋난 파일이 있으면 목록을 보여 주고 실패해요.

```bash
./mvnw spotless:check
```

편집기 쪽 들여쓰기는 `.editorconfig`가 맞춰 줘요.
```

이 커밋에서는 소스를 아직 정리하지 않아요.

**검증:** `./mvnw spotless:check`를 돌려요. 종료 코드가 0이 아니고 `The following files had format violations` 메시지와 함께 Java 파일 목록이 나와야 해요. 이 실패는 의도한 결과예요. 플러그인이 실제로 붙었고 규칙이 지금 코드와 다르다는 걸 확인하는 단계예요. `spotless:check`는 목표를 직접 부르는 방식이라 라이프사이클을 타지 않으니 이 명령만으로는 소스가 바뀌지 않아요.

## 커밋 2 — 전체 소스 서식 정리

작업 순서는 다음과 같아요.

1. 먼저 `./mvnw test`를 돌려서 정리 전 기준선을 기록해요. Testcontainers가 MySQL 8.4 컨테이너를 띄우니 Docker가 켜져 있어야 해요. 이 시점에 이미 깨지는 테스트가 있으면 그 목록을 적어 두세요. 서식 정리 탓으로 오해하지 않기 위해서예요. 참고로 `KisAuthClientManualTest`는 `@Disabled`라서 돌지 않아요.
2. `./mvnw spotless:apply`를 돌려요.
3. `git diff --stat`으로 Java 파일 14개만 바뀌었는지 확인해요. `pom.xml`이나 yaml이 목록에 있으면 설정이 잘못된 거예요.
4. 커밋 메시지는 `style: palantir-java-format 규칙으로 Java 소스 서식 정리`로 해요.

**검증:** 아래 두 명령이 모두 통과해야 해요.

```bash
./mvnw spotless:check   # 종료 코드 0, 위반 파일 없음
./mvnw test             # 1단계 기준선과 결과가 동일
```

## 실패 시 대처

`palantir-java-format 2.96.0`이 실행 중에 예외를 던지면(예: `IllegalAccessError`, 파싱 실패) `pom.xml`의 `palantir-java-format.version`을 spotless 3.9.0의 기본값인 `2.80.0`으로 낮춰서 한 번 더 시도해요. 그래도 실패하면 멈추고 오류 메시지를 그대로 보고하세요. 우회 설정을 추가하거나 다른 포매터로 바꾸는 판단은 하지 마세요.

## 기록할 피드백

`.harness/workspace/progress/argon1025-issue-6/feedback.md`에 아래 사실을 append하고, 해당 커밋에 함께 담아요.

- `trendlog-backend`의 Java 서식은 spotless의 palantir-java-format 단계가 코드 서식과 import 순서, 사용하지 않는 import 제거를 한꺼번에 처리해요. 그래서 `<removeUnusedImports/>`나 `<importOrder>`를 따로 넣으면 중복이고, import 정렬 규칙이 팀 취향과 다르다고 `<importOrder>`를 덧붙이면 palantir가 정렬한 결과를 다시 뒤집어 매 빌드마다 diff가 흔들려요.
- `trendlog-backend`는 `spotless:apply`를 Maven `process-sources` 단계에 묶었어요(사용자 결정). 그래서 `./mvnw test`나 `./mvnw spring-boot:run`이 작업 트리의 Java 파일을 실제로 고쳐 써요. 빌드가 소스를 바꾸지 않는다고 가정하지 마세요. 반대로 VS Code의 Java 확장은 Maven 라이프사이클을 타지 않아서 편집기 안 컴파일로는 서식이 적용되지 않아요.
- `trendlog-backend`는 git 훅과 GitHub Actions 서식 검사를 이번 범위에서 뺐어요(사용자 결정). 메이븐을 한 번도 돌리지 않고 커밋하면 서식이 어긋난 코드가 그대로 푸시될 수 있어요. 이 구멍을 막고 싶으면 `./mvnw spotless:install-git-pre-push-hook`을 각자 실행하면 되고, 이 훅은 푸시 때 `spotless:check`를 돌려 어긋나면 `apply`로 고친 뒤 푸시를 중단해요.
- `trendlog-backend`의 `.editorconfig`는 `[*.java]` 절만 두고 있어요. `pom.xml`은 Spring Initializr가 만든 탭 들여쓰기를 유지하기 때문에, 여기에 `[*.xml]`이나 `[*]` 절을 추가하면 pom 전체 diff가 발생해요.
  - evidence: .editorconfig
