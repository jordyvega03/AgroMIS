# Runbook: Triaje de Hallazgos de Seguridad

## Herramientas activas

| Herramienta | Frecuencia | Donde ver resultados |
|-------------|-----------|----------------------|
| Trivy Operator | Continuo (en cada nuevo pod) | `kubectl get vulnerabilityreports -A` |
| kube-bench | Semanal (lunes 4am) | Logs del CronJob en kube-system |
| Polaris | Continuo | Dashboard https://polaris.agromis.io |
| Trivy FS (CI) | Por PR | GitHub Actions checks |
| Gitleaks | Por PR | GitHub Actions checks |

## Niveles de severidad y SLA

| Severidad | SLA de remediacion | Accion |
|-----------|-------------------|--------|
| CRITICAL | 24 horas | Parche urgente + notificar a SRE |
| HIGH | 7 dias | Crear issue con prioridad alta |
| MEDIUM | 30 dias | Crear issue y planificar |
| LOW | Sin SLA | Backlog |

## Triage de vulnerabilidades en imagenes (Trivy)

```bash
# Ver todos los reportes de vulnerabilidades
kubectl get vulnerabilityreports -A

# Ver detalle de un reporte
kubectl describe vulnerabilityreport <nombre> -n <namespace>

# Filtrar CRITICAL y HIGH
kubectl get vulnerabilityreports -A \
  -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.report.summary.criticalCount}{"\t"}{.report.summary.highCount}{"\n"}{end}'
```

## Triage de kube-bench

```bash
# Ver logs del ultimo job
kubectl logs -n kube-system -l job-name=kube-bench --tail=1000 | python3 -c "
import sys, json
data = [json.loads(l) for l in sys.stdin if l.strip().startswith('{')]
for item in data:
    for test in item.get('tests', []):
        for result in test.get('results', []):
            if result.get('status') == 'FAIL':
                print(result.get('test_number'), result.get('test_desc'))
"
```

## Escalar hallazgo CRITICAL

1. Notificar en canal #security-alerts de Slack
2. Crear issue con label `security:critical`
3. Asignar a SRE on-call
4. Si el hallazgo es en produccion: activar protocolo de incidente
