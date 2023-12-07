import java
import random

ModuleConfig = java.jclass("com.example.trainerengine.configs.ModuleConfig")
ConfigData = java.jclass("com.example.trainerengine.configs.ConfigData")

Skill = java.jclass("com.example.trainerengine.skills.Skill")
SkillSet = java.jclass("com.example.trainerengine.skills.SkillSet")


def get_all_skills() -> dict[str, str]:
    return {"Mutiply10": "Multiply by 10",
            "Multiply0": "Multiply by 0",
            "Multiply1": "Multiply by 1",
            "MultiplyPosNeg": "Multiply positive by negative number",
            "MultiplyNegNeg": "Multiply negative by negative number"}


def make_task(config: ModuleConfig) -> tuple[str, int]:
    max_number = config.getConfigData("Max value").getValue()
    min_number = config.getConfigData("Min value").getValue()
    one = random.randint(min_number, max_number)
    two = random.randint(min_number, max_number)
    question = f"{one}×{two}="
    correct = one * two
    return question, correct


def check_answer(question: str, answer: int, correct: int, config: ModuleConfig) -> bool:
    return answer == correct
