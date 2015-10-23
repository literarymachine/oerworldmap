import csv, json, getopt, sys, urllib2, rdflib, pycountry, uuid, os

def convert(input_path, output_path):
    with open(input_path) as csvfile:
        servicereader = csv.reader(csvfile, delimiter=',', quotechar='"')
        # skip the first line
        next(servicereader)
        for row in servicereader:
            oerwm_id = 'urn:uuid:' + str(uuid.uuid1())
            try:
                country = pycountry.countries.get(name=row[8].strip()).alpha2
            except KeyError:
                country = None
            service = {
                "@context": "http://schema.org/",
                "@id": oerwm_id,
                "@type": "Service",
                "name": [
                    {
                        "@language": "en",
                        "@value": row[0].strip()
                    }
                ],
                "description": [
                    {
                        "@language": "en",
                        "@value": row[3].strip()
                    }
                ],
                "availableChannel": [
                    {
                        "@type": "ServiceChannel",
                        "serviceUrl": row[1].strip()
                    }
                ],
                "provider": {
                    "@type": "Organization",
                    "name": [
                        {
                            "@language": "en",
                            "@value": row[7].strip()
                        }
                    ],
                    "url": row[10].strip()
                },
            }
            if country:
                service["provider"]["location"] = {
                    "@type": "Place",
                    "address": {
                        "@type": "PostalAddress",
                        "addressCountry": country,
                    }
                }
            output_file = os.path.join(output_path, oerwm_id)
            with open(output_file, 'w') as file:
                json.dump(service, file, indent=2)
            print "Wrote data for " + oerwm_id

if __name__ == "__main__":
    input_path = ''
    output_path = ''
    try:
        opts, args = getopt.getopt(sys.argv[1:], "hi:o:", ["ifile=", "ofile="])
    except getopt.GetoptError:
        print '1.py -i <inputfile> -o <outputfile>'
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print '1.py -i <inputfile> -o <outputfile>'
            sys.exit()
        elif opt in ("-i", "--ifile"):
            input_path = arg
        elif opt in ("-o", "--ofile"):
            output_path = arg
    convert(input_path, output_path)
