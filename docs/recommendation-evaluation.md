# Recommendation evaluation

The repository includes a repeatable black-box evaluation that sends 150
requests to a running backend. It combines five Vancouver neighbourhoods, all
six Android discovery categories, and default, indoor, free, quiet, and
open-only variants.

Start MongoDB and the API, then run:

```bash
docker compose up -d
mvn spring-boot:run

./scripts/evaluate-recommendations.rb
```

The evaluator prints and writes `output/recommendation-evaluation.json`.
Override the endpoint or report path with `RECOMMENDATION_EVALUATION_BASE_URL`
and `RECOMMENDATION_EVALUATION_OUTPUT`.

The report measures HTTP failures, empty queries, latency, top-result success,
precision at five, category violations, unknown price/hour exposure, and a
nightlife-specific taxonomy check. These metrics are regression indicators;
they do not replace human judgment. Before release, maintain a reviewed set of
real query/result judgments and use it to define acceptable quality gates.
