input_file = 'data/cranqrel'
output_file = 'data/modified_cranqrel'

with open(input_file, 'r') as file:
    lines = file.readlines()

modified_lines = []
for line in lines:
    modified_line = line.replace('-1', '5')
    modified_lines.append(modified_line)

with open(output_file, 'w') as file:
    file.writelines(modified_lines)

