import os

directory = r"d:\vs project stuff\IT342_Basalo_Vigilo\backend\vigilo\src\main\java\edu\cit\basalo\vigilo\features"

for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith("Controller.java"):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            if "Principal principal" in content and "import java.security.Principal;" not in content:
                # Add import right after package declaration
                content = content.replace("package edu.cit.basalo.vigilo.features.", "package edu.cit.basalo.vigilo.features.")
                lines = content.split('\n')
                for i, line in enumerate(lines):
                    if line.startswith("package "):
                        lines.insert(i + 1, "\nimport java.security.Principal;")
                        break
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write('\n'.join(lines))
