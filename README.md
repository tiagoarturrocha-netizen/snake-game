# Snake (Android)

Jogo da cobrinha para Android, nativo (Java, sem dependências externas).

## Como gerar o APK

O workflow `.github/workflows/build-apk.yml` compila o APK automaticamente a cada push.
Após o workflow terminar, baixe o arquivo em **Actions → Build APK → snake-apk**
(é o `app-debug.apk`, instalável direto no celular com "fontes desconhecidas" ativada).

## Como jogar

- Deslize o dedo na tela para mudar a direção.
- Coma as frutas vermelhas para crescer e pontuar.
- Evite bater nas paredes e no próprio corpo.

## Build local (opcional)

Requisitos: JDK 17 e Gradle 8.7+.

```
gradle assembleDebug
```

O APK sai em `app/build/outputs/apk/debug/app-debug.apk`.
