# File paths for input and output
input_file = 'data/cranqrel'
output_file = 'data/cranqrel1'

# Read and modify the qrel file
with open(input_file, 'r') as file:
    lines = file.readlines()

# Correct the format and add '0' as the second column
corrected_lines = []
for line in lines:
    parts = line.strip().split()
    corrected_line = f"{parts[0]} 0 {parts[1]} {parts[2]}\n"
    corrected_lines.append(corrected_line)

# Write the corrected qrel to a new file
with open(output_file, 'w') as file:
    file.writelines(corrected_lines)

print("Qrel file has been updated with the correct format.")

