// Get the workspace directory from the environment
def workspace = System.getenv('WORKSPACE')

// Check if workspace is null
if (workspace == null) {
    throw new RuntimeException("Workspace directory not found")
}

// Read parameters from Jenkins
def nom = System.getenv('NOM')
def prenom = System.getenv('PRENOM')
def gender = System.getenv('GENRE')
def birth_date = System.getenv('DATE_NAISSANCE')
def email = System.getenv('EMAIL')
def numero = System.getenv('NUMERO')
def nir = System.getenv('NIR')

// Safely convert to boolean, defaulting to false if null
nom = (nom != null) ? nom.toBoolean() : false
prenom = (prenom != null) ? prenom.toBoolean() : false
gender = (gender != null) ? gender.toBoolean() : false
birth_date = (birth_date != null) ? birth_date.toBoolean() : false
email = (email != null) ? email.toBoolean() : false
numero = (numero != null) ? numero.toBoolean() : false
nir = (nir != null) ? nir.toBoolean() : false

// Start building the YAML content
def yamlContent = """
version: "1"
masking:
"""

//Ajoute un prénom
if (prenom) {
    yamlContent += """
  - selector:
      jsonpath: "first_name"
    masks:
      - add: ""
      - randomChoiceInUri: "pimo://nameFR"

"""
}

//Ajoute un nom
if (nom) {
    yamlContent += """
  - selector:
      jsonpath: "last_name"
    masks:
      - add: ""
      - randomChoiceInUri: "pimo://surnameFR"

"""
}

//Ajoute un genre
if (gender) {
    yamlContent += """
  - selector:
      jsonpath: "gender"
    masks:
      - add: ""
      - randomChoice:
        - "M"
        - "F"

"""
}

//Ajoute une date de naissance
if (birth_date) {
    yamlContent += """
  - selector:
      jsonpath: "birth_date"
    masks:
      - add: ""
      - randDate:
          dateMin: "1950-01-01T00:00:00Z"
          dateMax: "2020-01-01T00:00:00Z"
      - dateParser:
          outputFormat: "02/01/2006"

"""
}

//Ajoute une adresse email au format prenom.nom
//Doit avoir un nom et un prénom déjà définis pour que cela fonctionne
if (email) {
  yamlContent += """
  - selector:
      jsonpath: "email"
    mask:
      add: "{{ .first_name | lower | NoAccent }}.{{ .last_name | lower | NoAccent }}@yopmail.fr"

"""
}

//Ajoute un numéro de téléphone au format français
if (numero) {
    yamlContent += """
  - selector:
      jsonpath: "phone_fr"
    masks:
      - add: ""
      - regex: "0[1-6]( [0-9]{2}){4}"

"""
}


//Ajoute un numéro de sécurité social
//Doit avoir un genre et une date de naissance pour fonctionner
if (nir) {
    yamlContent += """
  - selector:
      jsonpath: "department_code"
    masks:
      - add: ""
      - randomInt:
          min: 1
          max: 99

  - selector:
      jsonpath: "city_code"
    masks:
      - add: ""
      - randomInt:
          min: 1
          max: 999

  - selector:
      jsonpath: "order"
    masks:
      - add: ""
      - randomInt:
          min: 1
          max: 999

  - selector:
      jsonpath: "nir_start"
    mask:
      add-transient: '{{if eq .gender "M" }}1{{else}}2{{end}}{{.birth_date | substr 8 10}}{{.birth_date | substr 3 5}}{{.department_code | printf "%02d"}}{{.city_code | printf "%03d"}}{{.order | printf "%03d"}}'
 
  - selector:
      jsonpath: "nir_control"
    mask:
      add-transient: '{{ sub 97 (mod (int64 .nir_start)  97)}}'

  - selector:
      jsonpath: "nir"
    mask:
      add: '{{.nir_start}}{{.nir_control}}'

"""
}

// Write the final YAML content to a file
def yamlFile = new File("${workspace}/generated.yml")
yamlFile.text = yamlContent
