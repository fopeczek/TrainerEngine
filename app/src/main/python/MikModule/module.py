import java
import random

ModuleConfig = java.jclass("com.example.trainerengine.configs.ModuleConfig")
ConfigData = java.jclass("com.example.trainerengine.configs.ConfigData")


def make_task(config: ModuleConfig) -> tuple[str, int]:
    max_number = config.getConfigData("Max value").getValue()
    min_number = config.getConfigData("Min value").getValue()
    one = random.randint(min_number, max_number)
    two = random.randint(min_number, max_number)
    neg = random.randint(0, 1)
    if neg == 0:
        question = f"{one}+{two}="
        correct = one + two
    else:
        question = f"{one}-{two}="
        correct = one - two
    return question, correct


def check_answer(question: str, answer: int, correct: int, config: ModuleConfig) -> bool:
    return answer == correct
