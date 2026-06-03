import base64, os

base_dir = r"E:\JavaProject\Corporate_OA_System\codeackend\oa-meeting\src\main\java\cn\oa\meeting"

files = {}
os.makedirs(os.path.join(base_dir, "service", "impl"), exist_ok=True)
os.makedirs(os.path.join(base_dir, "controller"), exist_ok=True)

for path, content in files.items():
    full_path = os.path.join(base_dir, path.replace("/", os.sep))
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content)
        print(f"Written: {full_path}")

print("All done")
