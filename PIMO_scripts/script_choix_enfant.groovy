import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Get the workspace directory from the environment
def workspace = build.getEnvironment(listener).get('WORKSPACE')

// Check if workspace is null
if (workspace == null) {
    throw new RuntimeException("Workspace directory not found")
}

// Read parameters from Jenkins
def parent = build.getEnvironment(listener).get('PARENT')
def enfant = build.getEnvironment(listener).get('ENFANT')
def age = build.getEnvironment(listener).get('AGE')

def dateMin
def dateMax


// Element du YAML
//Début du Yaml
def yamlContent = """
version: "1"
masking:

  - selector:
      jsonpath: "parent"
    mask:
      pipe:
        masking:
"""
//YAML pour père
def yamlPere = """
          - selector:
              jsonpath: "pere"
            mask:
              pipe:
                masking:
                  - selector:
                      jsonpath: "prenom"
                    masks:
                      - randomChoiceInUri: "pimo://nameFRM"
                  - selector:
                      jsonpath: "nom"
                    masks:
                      - randomChoiceInUri: "pimo://surnameFR"
"""
//YAML pour mère
def yamlMere = """
          - selector:
              jsonpath: "mere"
            mask:
              pipe:
                masking:
                  - selector:
                      jsonpath: "prenom"
                    masks:
                      - randomChoiceInUri: "pimo://nameFRF"
                  - selector:
                      jsonpath: "nom"
                    masks:
                      - randomChoiceInUri: "pimo://surnameFR"
"""

//Création début JSON
def jsonContent = "{\"parent\":[{"
def jsonPere = "\"pere\":[{\"nom\":\"\",\"prenom\":\"\"}]"
def jsonMere = "\"mere\":[{\"nom\":\"\",\"prenom\":\"\"}]"


//Ajoute un parent
if (parent == "Aléatoire") {
    def parent1 = "Homme"
    def parent2 = "Femme"

    def random = new Random()
    parent = (random.nextBoolean()) ? parent1 : parent2
}

if (parent == "Femme") {
    yamlContent += yamlMere
    jsonContent += jsonMere
}

if (parent == "Homme") {
    yamlContent += yamlPere
    jsonContent += jsonPere
}

//Transition du JSON
jsonContent += "}]"

//Ajoute une date de naissance
if (enfant == "Oui") {
  jsonContent += ",\"enfant\":[{\"prenom\":\"\",\"Date de naissance\":\"\"}]"

  // Create a variable for today's date
  def today = LocalDate.now()
  def dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  if (age == "agé de moins de 6 mois") {
    dateMin = today.minusMonths(6).format(dateFormatter)
    dateMax = today.minusDays(1).format(dateFormatter)
  }
  else if (age == "agé entre 6 mois et 18 ans") {
    dateMin = today.minusYears(18).format(dateFormatter)
    dateMax = today.minusMonths(6).format(dateFormatter)
  }
  else if (age == "agé de plus de 18 ans") {
    dateMin = today.minusYears(50).format(dateFormatter)
    dateMax = today.minusYears(18).format(dateFormatter)
  }

  yamlContent += """
  - selector:
      jsonpath: "enfant"
    mask:
      pipe:
        masking:
          - selector:
              jsonpath: "prenom"
            masks:
              - randomChoiceInUri: "pimo://nameFR"
          - selector:
              jsonpath: "Date de naissance"
            masks:
              - randDate:
                  dateMin: "${dateMin}T00:00:00Z"
                  dateMax: "${dateMax}T00:00:00Z"
              - dateParser:
                  outputFormat: "02/01/2006"
"""

}

//End JSON
jsonContent += "}"

// Write the final JSON content to a file
def jsonFile = new File("${workspace}/generatedJSON.json")
jsonFile.text = jsonContent

// Write the final YAML content to a file
def yamlFile = new File("${workspace}/generatedYAML.yml")
yamlFile.text = yamlContent
