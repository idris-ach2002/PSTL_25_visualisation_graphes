# Dépannage détaillé

## 1. Port occupé

```bash
docker ps
docker stop <id>
```

---

## 2. Page bloquée

```bash
./run_app.sh fresh
```

Puis `Ctrl + F5`.

---

## 3. WebGL indisponible

Tester :

- autre navigateur ;
- accélération matérielle ;
- mode 2D ;
- limite plus basse.

---

## 4. npm lent

Vérifier :

```bash
cat frontend/.npmrc
```

---

## 5. WASM non chargé

Vérifier dans l'onglet Network du navigateur que le `.wasm` est servi.

---

## 6. Import refusé

Réduire le graphe ou modifier la limite.

---

## 7. Docker cache une ancienne version

```bash
docker compose build --no-cache graph-web
```
