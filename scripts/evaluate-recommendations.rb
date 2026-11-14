#!/usr/bin/env ruby
require "json"
require "fileutils"
require "net/http"
require "time"
require "uri"

base_url = ENV.fetch("RECOMMENDATION_EVALUATION_BASE_URL", "http://127.0.0.1:8080")
output_file = ENV.fetch(
  "RECOMMENDATION_EVALUATION_OUTPUT",
  "output/recommendation-evaluation.json"
)
locations = {
  "downtown" => [49.2827, -123.1207],
  "gastown" => [49.2833, -123.1065],
  "kitsilano" => [49.2684, -123.1683],
  "commercial_drive" => [49.2624, -123.0693],
  "west_end" => [49.2866, -123.1340]
}
categories = {
  "cafe" => "cafe",
  "food" => "restaurant",
  "park" => "park",
  "study" => "study",
  "shopping" => "shopping",
  "nightlife" => "nightlife"
}
variants = {
  "default" => {},
  "indoor" => {"indoorPreferred" => "true"},
  "free" => {"budgetPreference" => "free"},
  "quiet" => {"atmosphere" => "quiet"},
  "open_only" => {"openOnly" => "true"}
}

rows = []
locations.each do |location, (lat, lng)|
  categories.each do |input_category, desired_category|
    variants.each do |variant, extras|
      parameters = {
        "lat" => lat,
        "lng" => lng,
        "category" => input_category,
        "maxWalkingMinutes" => 20,
        "limit" => 10
      }.merge(extras)
      uri = URI("#{base_url}/api/v1/recommendations")
      uri.query = URI.encode_www_form(parameters)
      started = Process.clock_gettime(Process::CLOCK_MONOTONIC)
      response = Net::HTTP.get_response(uri)
      elapsed = (Process.clock_gettime(Process::CLOCK_MONOTONIC) - started) * 1_000
      results = response.code == "200" ? JSON.parse(response.body) : []
      rows << {
        "location" => location,
        "inputCategory" => input_category,
        "desiredCategory" => desired_category,
        "variant" => variant,
        "status" => response.code.to_i,
        "latencyMs" => elapsed.round(1),
        "results" => results
      }
    end
  end
end

def relevant?(place, desired)
  place["category"] == desired
end

def genuine_nightlife?(place)
  tags = place["tags"] || []
  tags.any? do |tag|
    ["bar", "pub", "nightclub", "music-venue", "lounge", "brewery",
     "beer-garden", "cocktail-bar", "wine-bar"].include?(tag)
  end
end

summary = {
  "generatedAt" => Time.now.utc.iso8601,
  "queryCount" => rows.size,
  "httpFailures" => rows.count { |row| row["status"] != 200 },
  "emptyQueries" => rows.count { |row| row["results"].empty? },
  "latencyMs" => {
    "average" => (rows.sum { |row| row["latencyMs"] } / rows.size).round(1),
    "maximum" => rows.map { |row| row["latencyMs"] }.max
  },
  "categories" => {},
  "variants" => {},
  "nightlife" => {}
}

categories.each do |input, desired|
  subset = rows.select { |row| row["inputCategory"] == input }
  top_five = subset.flat_map { |row| row["results"].first(5) }
  summary["categories"][input] = {
    "queries" => subset.size,
    "top1RelevantPercent" => (100.0 * subset.count { |row|
      row["results"].first && relevant?(row["results"].first, desired)
    } / subset.size).round(1),
    "precisionAt5Percent" => top_five.empty? ? 0 :
      (100.0 * top_five.count { |place| relevant?(place, desired) } /
        top_five.size).round(1),
    "queriesWithNoRelevantTop5" => subset.count { |row|
      row["results"].first(5).none? { |place| relevant?(place, desired) }
    }
  }
end

variants.each_key do |variant|
  subset = rows.select { |row| row["variant"] == variant }
  top_five = subset.flat_map { |row| row["results"].first(5) }
  summary["variants"][variant] = {
    "queries" => subset.size,
    "top5Results" => top_five.size,
    "categoryRelevantPercent" => top_five.empty? ? 0 :
      (100.0 * subset.sum { |row|
        row["results"].first(5).count { |place|
          relevant?(place, row["desiredCategory"])
        }
      } / top_five.size).round(1),
    "indoorKnownAndTruePercent" => top_five.empty? ? 0 :
      (100.0 * top_five.count { |place|
        place["indoorKnown"] && place["indoor"]
      } / top_five.size).round(1),
    "priceUnknownPercent" => top_five.empty? ? 0 :
      (100.0 * top_five.count { |place| !place["priceKnown"] } /
        top_five.size).round(1),
    "quietMatchPercent" => top_five.empty? ? 0 :
      (100.0 * top_five.count { |place|
        (place["reasons"] || []).any? { |reason| reason.include?("quiet atmosphere") }
      } / top_five.size).round(1),
    "hoursUnknownPercent" => top_five.empty? ? 0 :
      (100.0 * top_five.count { |place| !place["hoursKnown"] } /
        top_five.size).round(1)
  }
end

nightlife = rows.select { |row| row["inputCategory"] == "nightlife" }
nightlife_top_five = nightlife.flat_map { |row| row["results"].first(5) }
summary["nightlife"] = {
  "top5Results" => nightlife_top_five.size,
  "genuineNightlifePercent" => nightlife_top_five.empty? ? 0 :
    (100.0 * nightlife_top_five.count { |place|
      genuine_nightlife?(place)
    } / nightlife_top_five.size).round(1),
  "suspiciousExamples" => nightlife_top_five.reject { |place|
    genuine_nightlife?(place)
  }.uniq { |place| place["placeId"] }.first(15).map { |place|
    {"name" => place["name"], "description" => place["description"],
     "score" => place["score"]}
  }
}

FileUtils.mkdir_p(File.dirname(output_file)) unless File.dirname(output_file) == "."
File.write(output_file, JSON.pretty_generate(summary) + "\n")
puts JSON.pretty_generate(summary)
